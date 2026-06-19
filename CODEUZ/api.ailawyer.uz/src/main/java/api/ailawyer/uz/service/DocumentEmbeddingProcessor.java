package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.exps.GeminiApiException;
import api.ailawyer.uz.repository.LawChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Hujjat chunklarini fonda Gemini orqali vektorlaydi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEmbeddingProcessor {

    private final LawChunkRepository lawChunkRepository;
    private final GeminiEmbeddingService geminiEmbeddingService;

    @Value("${gemini.embedding.delay-ms:500}")
    private long embeddingDelayMs;

    @Async("embeddingExecutor")
    public void processEmbeddingsForDocument(UUID documentId) {
        List<LawChunkEntity> pendingChunks = lawChunkRepository.findAllByDocumentIdAndEmbeddingIsNull(documentId);

        if (pendingChunks.isEmpty()) {
            log.info("Embedding kerak emas documentId={}", documentId);
            return;
        }

        log.info("Embedding boshlandi documentId={}, pendingCount={}", documentId, pendingChunks.size());

        int successCount = 0;
        int failureCount = 0;

        for (LawChunkEntity chunk : pendingChunks) {
            try {
                float[] embedding = geminiEmbeddingService.getEmbedding(chunk.getContent());
                chunk.setEmbedding(embedding);
                lawChunkRepository.save(chunk);
                successCount++;
            } catch (GeminiApiException e) {
                failureCount++;
                log.error("Chunk embedding xatosi documentId={}, articleRef={}: {}",
                        documentId, chunk.getArticleRef(), e.getMessage());
            }

            sleepBetweenRequests();
        }

        log.info("Embedding yakunlandi documentId={}, success={}, failed={}",
                documentId, successCount, failureCount);
    }

    private void sleepBetweenRequests() {
        if (embeddingDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(embeddingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Embedding delay interrupt qilindi");
        }
    }
}
