package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentType;
import api.ailawyer.uz.exps.AppBadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * .docx huquqiy hujjatlarni ierarxik (bob/modda/band) bo'laklarga ajratadi.
 */
@Service
@Slf4j
public class LegalDocumentParsingService {

    private static final Pattern BOB_PATTERN =
            Pattern.compile("(?i)^\\s*(\\d+)\\s*[-–—.]?\\s*BOB\\b[\\.:]?\\s*(.*)$");
    private static final Pattern MODDA_PATTERN =
            Pattern.compile("(?i)^\\s*(\\d+)\\s*[-–—.]?\\s*modda\\b[\\.:]?\\s*(.*)$");
    private static final Pattern BAND_PATTERN =
            Pattern.compile("(?i)^\\s*(\\d+)\\s*[-–—.]?\\s*band\\b[\\.:]?\\s*(.*)$");
    private static final Pattern NUMBERED_POINT_PATTERN =
            Pattern.compile("^\\s*(\\d+)\\s*[.)]\\s+(.+)$");

    public List<LawChunkEntity> parseAndChunkDocx(MultipartFile file, LegalDocumentEntity document) {
        validateFile(file);

        try (InputStream inputStream = file.getInputStream();
             XWPFDocument docx = new XWPFDocument(inputStream)) {

            ParsingContext context = new ParsingContext();
            if (isCodeOrLaw(document.getType())) {
                parseCodeOrLawDocument(docx, document, context);
            } else {
                parseResolutionStyleDocument(docx, document, context);
            }

            context.finalizeCurrentChunk(document);

            if (context.chunks.isEmpty()) {
                throw new AppBadException("Hujjatdan modda yoki band ajratib bo'lmadi!");
            }

            log.info("Hujjat parsing yakunlandi docNumber={}, chunkCount={}",
                    document.getDocNumber(), context.chunks.size());
            return context.chunks;
        } catch (IOException e) {
            throw new AppBadException("Word hujjatini o'qishda xatolik: " + e.getMessage());
        }
    }

    private void parseCodeOrLawDocument(XWPFDocument docx, LegalDocumentEntity document, ParsingContext context) {
        for (IBodyElement element : docx.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                handleCodeOrLawParagraph(paragraph, document, context);
            } else if (element instanceof XWPFTable table) {
                handleTable(table, context);
            }
        }
    }

    private void parseResolutionStyleDocument(XWPFDocument docx, LegalDocumentEntity document, ParsingContext context) {
        for (IBodyElement element : docx.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                handleResolutionParagraph(paragraph, document, context);
            } else if (element instanceof XWPFTable table) {
                handleTable(table, context);
            }
        }
    }

    private void handleCodeOrLawParagraph(XWPFParagraph paragraph, LegalDocumentEntity document, ParsingContext context) {
        String text = extractParagraphText(paragraph);
        if (text.isBlank()) {
            return;
        }

        Matcher bobMatcher = BOB_PATTERN.matcher(text);
        if (bobMatcher.find()) {
            context.currentChapter = text;
            return;
        }

        Matcher moddaMatcher = MODDA_PATTERN.matcher(text);
        if (moddaMatcher.find()) {
            context.finalizeCurrentChunk(document);
            context.currentArticleRef = moddaMatcher.group(1) + "-modda";
            context.appendLine(text);
            return;
        }

        if (context.hasOpenChunk()) {
            context.appendLine(text);
        } else {
            context.ensurePrefaceChunk(document);
            context.appendLine(text);
        }
    }

    private void handleResolutionParagraph(XWPFParagraph paragraph, LegalDocumentEntity document, ParsingContext context) {
        String text = extractParagraphText(paragraph);
        if (text.isBlank()) {
            return;
        }

        Matcher bobMatcher = BOB_PATTERN.matcher(text);
        if (bobMatcher.find()) {
            context.finalizeCurrentChunk(document);
            context.currentChapter = text;
            return;
        }

        Matcher bandMatcher = BAND_PATTERN.matcher(text);
        if (bandMatcher.find()) {
            context.finalizeCurrentChunk(document);
            context.currentArticleRef = buildBandArticleRef(context.currentChapter, bandMatcher.group(1));
            context.appendLine(text);
            return;
        }

        Matcher numberedMatcher = NUMBERED_POINT_PATTERN.matcher(text);
        if (numberedMatcher.find()) {
            context.finalizeCurrentChunk(document);
            context.currentArticleRef = buildBandArticleRef(context.currentChapter, numberedMatcher.group(1));
            context.appendLine(text);
            return;
        }

        if (context.hasOpenChunk()) {
            context.appendLine(text);
        } else if (!context.currentChapter.isBlank()) {
            context.currentArticleRef = extractBobRef(context.currentChapter);
            context.appendLine(text);
        } else {
            context.ensurePrefaceChunk(document);
            context.appendLine(text);
        }
    }

    private void handleTable(XWPFTable table, ParsingContext context) {
        if (!context.hasOpenChunk()) {
            return;
        }
        context.appendLine(convertTableToMarkdown(table));
    }

    private String convertTableToMarkdown(XWPFTable table) {
        StringBuilder markdown = new StringBuilder();
        List<XWPFTableRow> rows = table.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            XWPFTableRow row = rows.get(rowIndex);
            markdown.append("|");
            for (XWPFTableCell cell : row.getTableCells()) {
                markdown.append(" ")
                        .append(sanitizeTableCell(cell.getText()))
                        .append(" |");
            }
            markdown.append("\n");

            if (rowIndex == 0) {
                markdown.append("|");
                for (int i = 0; i < row.getTableCells().size(); i++) {
                    markdown.append(" --- |");
                }
                markdown.append("\n");
            }
        }
        return markdown.toString().trim();
    }

    private String sanitizeTableCell(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('|', '/').trim();
    }

    private String extractParagraphText(XWPFParagraph paragraph) {
        String text = paragraph.getText();
        return text == null ? "" : text.trim();
    }

    private boolean isCodeOrLaw(DocumentType type) {
        return type == DocumentType.CODE || type == DocumentType.LAW;
    }

    private String buildBandArticleRef(String currentChapter, String bandNumber) {
        String bobRef = extractBobRef(currentChapter);
        String bandRef = bandNumber + "-band";
        if (bobRef.isBlank()) {
            return bandRef;
        }
        return bobRef + ", " + bandRef;
    }

    private String extractBobRef(String chapterText) {
        if (chapterText == null || chapterText.isBlank()) {
            return "";
        }
        Matcher matcher = BOB_PATTERN.matcher(chapterText);
        if (matcher.find()) {
            return matcher.group(1) + "-bob";
        }
        return chapterText.length() <= 64 ? chapterText : chapterText.substring(0, 64);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppBadException("Yuklanadigan fayl bo'sh!");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new AppBadException("Faqat .docx formatidagi Word hujjat qabul qilinadi!");
        }
    }

    String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 mavjud emas", e);
        }
    }

    private String normalizeArticleRef(String articleRef) {
        if (articleRef == null || articleRef.isBlank()) {
            return "preface";
        }
        return articleRef.length() <= 64 ? articleRef : articleRef.substring(0, 64);
    }

    private String buildChunkContent(String currentChapter, StringBuilder body) {
        String bodyText = body.toString().trim();
        if (currentChapter == null || currentChapter.isBlank()) {
            return bodyText;
        }
        if (bodyText.isBlank()) {
            return currentChapter.trim();
        }
        return currentChapter.trim() + "\n\n" + bodyText;
    }

    private LawChunkEntity createChunk(LegalDocumentEntity document, String articleRef, String content) {
        LawChunkEntity chunk = new LawChunkEntity();
        chunk.setDocumentId(document.getId());
        chunk.setDocument(document);
        chunk.setArticleRef(normalizeArticleRef(articleRef));
        chunk.setContent(content);
        chunk.setTextHash(sha256(content));
        chunk.setEmbedding(null);
        return chunk;
    }

    private class ParsingContext {
        private String currentChapter = "";
        private String currentArticleRef = "preface";
        private final StringBuilder currentContent = new StringBuilder();
        private final List<LawChunkEntity> chunks = new ArrayList<>();

        void appendLine(String line) {
            if (currentContent.length() > 0) {
                currentContent.append('\n');
            }
            currentContent.append(line);
        }

        boolean hasOpenChunk() {
            return currentContent.length() > 0;
        }

        void ensurePrefaceChunk(LegalDocumentEntity document) {
            if (!hasOpenChunk() && "preface".equals(currentArticleRef)) {
                currentArticleRef = "preface";
            }
        }

        void finalizeCurrentChunk(LegalDocumentEntity document) {
            if (!hasOpenChunk()) {
                return;
            }
            String content = buildChunkContent(currentChapter, currentContent);
            if (!content.isBlank()) {
                chunks.add(createChunk(document, currentArticleRef, content));
            }
            currentContent.setLength(0);
        }
    }
}
