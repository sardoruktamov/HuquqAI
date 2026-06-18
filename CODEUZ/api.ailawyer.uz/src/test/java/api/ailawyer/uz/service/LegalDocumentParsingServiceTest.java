package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalDocumentParsingServiceTest {

    private final LegalDocumentParsingService parsingService = new LegalDocumentParsingService();

    @Test
    void sha256_generatesConsistentHash() {
        String hash1 = parsingService.sha256("test content");
        String hash2 = parsingService.sha256("test content");
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void parseAndChunkDocx_splitsCodeByModdaAndPrependsChapter() throws Exception {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph chapter = document.createParagraph();
        chapter.createRun().setText("1-BOB. Umumiy qoidalar");

        XWPFParagraph modda1 = document.createParagraph();
        modda1.createRun().setText("1-modda. Birinchi modda matni.");

        XWPFParagraph modda1Body = document.createParagraph();
        modda1Body.createRun().setText("Modda davomi.");

        XWPFParagraph modda2 = document.createParagraph();
        modda2.createRun().setText("2-modda. Ikkinchi modda matni.");

        byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.write(out);
            bytes = out.toByteArray();
        }
        document.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mehnat-kodeksi.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                bytes
        );

        LegalDocumentEntity legalDocument = new LegalDocumentEntity();
        legalDocument.setId(java.util.UUID.randomUUID());
        legalDocument.setType(DocumentType.CODE);
        legalDocument.setDocNumber("MK-001");
        legalDocument.setTitle("Mehnat kodeksi");

        List<LawChunkEntity> chunks = parsingService.parseAndChunkDocx(file, legalDocument);

        assertEquals(2, chunks.size());
        assertEquals("1-modda", chunks.get(0).getArticleRef());
        assertTrue(chunks.get(0).getContent().contains("1-BOB. Umumiy qoidalar"));
        assertTrue(chunks.get(0).getContent().contains("Modda davomi."));
        assertEquals("2-modda", chunks.get(1).getArticleRef());
        assertNotNull(chunks.get(0).getTextHash());
        assertFalse(chunks.get(0).getTextHash().isBlank());
    }

    @Test
    void parseAndChunkDocx_splitsResolutionByBobAndBand() throws Exception {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph bob = document.createParagraph();
        bob.createRun().setText("1-BOB. Umumiy qoidalar");

        XWPFParagraph band1 = document.createParagraph();
        band1.createRun().setText("1-band. Birinchi band.");

        XWPFParagraph band2 = document.createParagraph();
        band2.createRun().setText("2-band. Ikkinchi band.");

        byte[] bytes;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.write(out);
            bytes = out.toByteArray();
        }
        document.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vmq-370.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                bytes
        );

        LegalDocumentEntity legalDocument = new LegalDocumentEntity();
        legalDocument.setId(java.util.UUID.randomUUID());
        legalDocument.setType(DocumentType.CABINET_RESOLUTION);
        legalDocument.setDocNumber("VMQ-370");
        legalDocument.setTitle("VMQ 370");

        List<LawChunkEntity> chunks = parsingService.parseAndChunkDocx(file, legalDocument);

        assertEquals(2, chunks.size());
        assertEquals("1-bob, 1-band", chunks.get(0).getArticleRef());
        assertEquals("1-bob, 2-band", chunks.get(1).getArticleRef());
        assertTrue(chunks.get(0).getContent().contains("1-BOB. Umumiy qoidalar"));
    }
}
