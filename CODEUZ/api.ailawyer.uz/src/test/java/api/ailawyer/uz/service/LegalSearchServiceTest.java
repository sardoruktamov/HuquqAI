package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.repository.LawChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalSearchServiceTest {

    @Mock
    private GeminiEmbeddingService geminiEmbeddingService;

    @Mock
    private LawChunkRepository lawChunkRepository;

    @InjectMocks
    private LegalSearchService legalSearchService;

    @Test
    void searchRelevantContext_returnsEmptyWhenQuestionBlank() {
        List<LawChunkEntity> result = legalSearchService.searchRelevantContext("  ", 5);

        assertTrue(result.isEmpty());
        verify(geminiEmbeddingService, never()).getEmbedding(any());
        verify(lawChunkRepository, never()).findSimilarChunks(any(), eq(5));
    }

    @Test
    void searchRelevantContext_returnsEmptyWhenEmbeddingEmpty() {
        when(geminiEmbeddingService.getEmbedding("savol")).thenReturn(new float[0]);

        List<LawChunkEntity> result = legalSearchService.searchRelevantContext("savol", 5);

        assertTrue(result.isEmpty());
        verify(lawChunkRepository, never()).findSimilarChunks(any(), eq(5));
    }

    @Test
    void searchRelevantContext_delegatesToRepositoryWhenEmbeddingPresent() {
        float[] vector = {0.1f, 0.2f};
        LawChunkEntity chunk = new LawChunkEntity();
        chunk.setId(UUID.randomUUID());
        when(geminiEmbeddingService.getEmbedding("savol")).thenReturn(vector);
        when(lawChunkRepository.findSimilarChunks(vector, 3)).thenReturn(List.of(chunk));

        List<LawChunkEntity> result = legalSearchService.searchRelevantContext("savol", 3);

        assertEquals(1, result.size());
        verify(lawChunkRepository).findSimilarChunks(vector, 3);
    }

    @Test
    void searchRelevantContext_returnsEmptyListFromRepository() {
        float[] vector = {0.5f};
        when(geminiEmbeddingService.getEmbedding("savol")).thenReturn(vector);
        when(lawChunkRepository.findSimilarChunks(vector, 5)).thenReturn(Collections.emptyList());

        List<LawChunkEntity> result = legalSearchService.searchRelevantContext("savol", 5);

        assertTrue(result.isEmpty());
    }
}
