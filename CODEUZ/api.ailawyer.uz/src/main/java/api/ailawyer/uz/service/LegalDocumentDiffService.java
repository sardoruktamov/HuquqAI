package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentStatus;
import api.ailawyer.uz.repository.LawChunkRepository;
import api.ailawyer.uz.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Yangi hujjat versiyasini mavjud chunklar bilan hash orqali solishtiradi
 * va faqat o'zgargan, yangi yoki o'chirilgan moddalarni yangilaydi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentDiffService {

    private final LawChunkRepository lawChunkRepository;
    private final LegalDocumentRepository legalDocumentRepository;

    @Transactional
    public void processDocumentUpdate(LegalDocumentEntity existingDoc, List<LawChunkEntity> newlyParsedChunks) {
        UUID documentId = existingDoc.getId();

        Map<String, LawChunkEntity> existingByArticleRef = lawChunkRepository
                .findAllByDocumentIdOrderByArticleRefAsc(documentId)
                .stream()
                .collect(Collectors.toMap(
                        LawChunkEntity::getArticleRef,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<LawChunkEntity> chunksToSave = new ArrayList<>();
        List<LawChunkEntity> chunksToDelete = new ArrayList<>();

        for (LawChunkEntity newChunk : newlyParsedChunks) {
            LawChunkEntity existingChunk = existingByArticleRef.remove(newChunk.getArticleRef());

            if (existingChunk != null) {
                if (existingChunk.getTextHash().equals(newChunk.getTextHash())) {
                    continue;
                }
                existingChunk.setContent(newChunk.getContent());
                existingChunk.setTextHash(newChunk.getTextHash());
                existingChunk.setEmbedding(null);
                chunksToSave.add(existingChunk);
                continue;
            }

            newChunk.setDocumentId(documentId);
            newChunk.setDocument(existingDoc);
            newChunk.setEmbedding(null);
            chunksToSave.add(newChunk);
        }

        chunksToDelete.addAll(existingByArticleRef.values());

        if (!chunksToDelete.isEmpty()) {
            lawChunkRepository.deleteAll(chunksToDelete);
        }
        if (!chunksToSave.isEmpty()) {
            lawChunkRepository.saveAll(chunksToSave);
        }

        if (!chunksToSave.isEmpty() || !chunksToDelete.isEmpty()) {
            existingDoc.setStatus(DocumentStatus.PARTIALLY_AMENDED);
            existingDoc.setUpdatedAt(LocalDateTime.now());
            legalDocumentRepository.save(existingDoc);

            log.info("Hujjat hash-diff yangilandi docNumber={}, saved={}, deleted={}, unchanged={}",
                    existingDoc.getDocNumber(),
                    chunksToSave.size(),
                    chunksToDelete.size(),
                    newlyParsedChunks.size() - chunksToSave.size());
        } else {
            log.info("Hujjat hash-diff: o'zgarish topilmadi docNumber={}", existingDoc.getDocNumber());
        }
    }
}
