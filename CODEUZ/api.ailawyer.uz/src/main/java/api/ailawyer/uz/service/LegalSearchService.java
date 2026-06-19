package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.repository.LawChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Huquqiy hujjat bo'laklarini cosine similarity bo'yicha qidiradi (RAG retrieval).
 */
@Service
@RequiredArgsConstructor
public class LegalSearchService {

    private final GeminiEmbeddingService geminiEmbeddingService;
    private final LawChunkRepository lawChunkRepository;

    public List<LawChunkEntity> searchRelevantContext(String userQuestion, int topK) {
        if (userQuestion == null || userQuestion.isBlank()) {
            return Collections.emptyList();
        }

        float[] vector = geminiEmbeddingService.getEmbedding(userQuestion);
        if (vector == null || vector.length == 0) {
            return Collections.emptyList();
        }

        return lawChunkRepository.findSimilarChunks(vector, topK);
    }
}
