package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentStatus;
import api.ailawyer.uz.repository.LawChunkRepository;
import api.ailawyer.uz.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentDiffServiceTest {

    @Mock
    private LawChunkRepository lawChunkRepository;

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @InjectMocks
    private LegalDocumentDiffService legalDocumentDiffService;

    @Test
    void processDocumentUpdate_doesNothingWhenHashesMatch() {
        UUID documentId = UUID.randomUUID();
        LegalDocumentEntity document = document(documentId, "MK-001");

        LawChunkEntity existing = chunk(documentId, "1-modda", "same content", "hash-a");
        LawChunkEntity parsed = chunk(documentId, "1-modda", "same content", "hash-a");

        when(lawChunkRepository.findAllByDocumentIdOrderByArticleRefAsc(documentId))
                .thenReturn(List.of(existing));

        legalDocumentDiffService.processDocumentUpdate(document, List.of(parsed));

        verify(lawChunkRepository, never()).saveAll(any());
        verify(lawChunkRepository, never()).deleteAll(any());
        verify(legalDocumentRepository, never()).save(any());
    }

    @Test
    void processDocumentUpdate_updatesChangedChunkAndMarksDocumentAmended() {
        UUID documentId = UUID.randomUUID();
        LegalDocumentEntity document = document(documentId, "MK-001");

        LawChunkEntity existing1 = chunk(documentId, "1-modda", "old one", "hash-old-1");
        LawChunkEntity existing2 = chunk(documentId, "2-modda", "same two", "hash-two");
        LawChunkEntity parsed1 = chunk(documentId, "1-modda", "new one", "hash-new-1");
        LawChunkEntity parsed2 = chunk(documentId, "2-modda", "same two", "hash-two");
        LawChunkEntity parsed3 = chunk(documentId, "3-modda", "brand new", "hash-three");

        when(lawChunkRepository.findAllByDocumentIdOrderByArticleRefAsc(documentId))
                .thenReturn(new ArrayList<>(List.of(existing1, existing2)));

        legalDocumentDiffService.processDocumentUpdate(document, List.of(parsed1, parsed2, parsed3));

        ArgumentCaptor<List<LawChunkEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(lawChunkRepository).saveAll(saveCaptor.capture());

        List<LawChunkEntity> saved = saveCaptor.getValue();
        assertEquals(2, saved.size());
        assertEquals("new one", existing1.getContent());
        assertEquals("hash-new-1", existing1.getTextHash());
        assertNull(existing1.getEmbedding());
        assertEquals("3-modda", parsed3.getArticleRef());
        assertEquals(documentId, parsed3.getDocumentId());

        verify(lawChunkRepository, never()).deleteAll(any());
        verify(legalDocumentRepository).save(document);
        assertEquals(DocumentStatus.PARTIALLY_AMENDED, document.getStatus());
    }

    @Test
    void processDocumentUpdate_deletesRemovedChunks() {
        UUID documentId = UUID.randomUUID();
        LegalDocumentEntity document = document(documentId, "VMQ-370");

        LawChunkEntity existing1 = chunk(documentId, "1-band", "band one", "hash-1");
        LawChunkEntity existing2 = chunk(documentId, "2-band", "band two", "hash-2");
        LawChunkEntity parsed1 = chunk(documentId, "1-band", "band one", "hash-1");

        when(lawChunkRepository.findAllByDocumentIdOrderByArticleRefAsc(documentId))
                .thenReturn(new ArrayList<>(List.of(existing1, existing2)));

        legalDocumentDiffService.processDocumentUpdate(document, List.of(parsed1));

        ArgumentCaptor<List<LawChunkEntity>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(lawChunkRepository).deleteAll(deleteCaptor.capture());
        assertEquals(1, deleteCaptor.getValue().size());
        assertEquals("2-band", deleteCaptor.getValue().get(0).getArticleRef());

        verify(lawChunkRepository, never()).saveAll(any());
        verify(legalDocumentRepository).save(document);
        assertEquals(DocumentStatus.PARTIALLY_AMENDED, document.getStatus());
    }

    private LegalDocumentEntity document(UUID id, String docNumber) {
        LegalDocumentEntity document = new LegalDocumentEntity();
        document.setId(id);
        document.setDocNumber(docNumber);
        document.setStatus(DocumentStatus.ACTIVE);
        return document;
    }

    private LawChunkEntity chunk(UUID documentId, String articleRef, String content, String textHash) {
        LawChunkEntity chunk = new LawChunkEntity();
        chunk.setId(UUID.randomUUID());
        chunk.setDocumentId(documentId);
        chunk.setArticleRef(articleRef);
        chunk.setContent(content);
        chunk.setTextHash(textHash);
        chunk.setEmbedding(new float[] {0.1f, 0.2f});
        return chunk;
    }
}
