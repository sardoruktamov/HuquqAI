package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.exps.GeminiApiException;
import api.ailawyer.uz.repository.LawChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingProcessorTest {

    @Mock
    private LawChunkRepository lawChunkRepository;

    @Mock
    private GeminiEmbeddingService geminiEmbeddingService;

    @InjectMocks
    private DocumentEmbeddingProcessor documentEmbeddingProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentEmbeddingProcessor, "embeddingDelayMs", 0L);
    }

    @Test
    void processEmbeddingsForDocument_embedsPendingChunks() {
        UUID documentId = UUID.randomUUID();

        LawChunkEntity chunk = new LawChunkEntity();
        chunk.setArticleRef("1-modda");
        chunk.setContent("Modda matni");

        when(lawChunkRepository.findAllByDocumentIdAndEmbeddingIsNull(documentId)).thenReturn(List.of(chunk));
        when(geminiEmbeddingService.getEmbedding("Modda matni")).thenReturn(new float[] {0.5f, 0.6f});

        documentEmbeddingProcessor.processEmbeddingsForDocument(documentId);

        verify(geminiEmbeddingService).getEmbedding("Modda matni");
        verify(lawChunkRepository).save(chunk);
    }

    @Test
    void processEmbeddingsForDocument_skipsSaveWhenEmbeddingFails() {
        UUID documentId = UUID.randomUUID();

        LawChunkEntity chunk = new LawChunkEntity();
        chunk.setArticleRef("2-modda");
        chunk.setContent("Xato modda");

        when(lawChunkRepository.findAllByDocumentIdAndEmbeddingIsNull(documentId)).thenReturn(List.of(chunk));
        when(geminiEmbeddingService.getEmbedding("Xato modda"))
                .thenThrow(new GeminiApiException("Rate limit"));

        documentEmbeddingProcessor.processEmbeddingsForDocument(documentId);

        verify(lawChunkRepository, never()).save(any());
    }
}
