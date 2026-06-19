package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.legal.LegalDocumentUploadDTO;
import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentStatus;
import api.ailawyer.uz.enums.DocumentType;
import api.ailawyer.uz.repository.LawChunkRepository;
import api.ailawyer.uz.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private LawChunkRepository lawChunkRepository;

    @Mock
    private LegalDocumentParsingService legalDocumentParsingService;

    @Mock
    private LegalDocumentDiffService legalDocumentDiffService;

    @Mock
    private DocumentEmbeddingProcessor documentEmbeddingProcessor;

    @InjectMocks
    private LegalDocumentService legalDocumentService;

    @Test
    void upload_existingDocumentUsesDiffServiceInsteadOfCreatingNew() {
        UUID documentId = UUID.randomUUID();
        LegalDocumentEntity existing = new LegalDocumentEntity();
        existing.setId(documentId);
        existing.setDocNumber("MK-001");
        existing.setTitle("Old title");
        existing.setType(DocumentType.CODE);
        existing.setStatus(DocumentStatus.ACTIVE);

        LegalDocumentUploadDTO dto = new LegalDocumentUploadDTO();
        dto.setType(DocumentType.CODE);
        dto.setDocNumber("MK-001");
        dto.setTitle("New title");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mehnat-kodeksi.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1, 2, 3}
        );

        LawChunkEntity parsedChunk = new LawChunkEntity();
        parsedChunk.setArticleRef("1-modda");

        when(legalDocumentRepository.findByDocNumber("MK-001")).thenReturn(Optional.of(existing));
        when(legalDocumentRepository.save(existing)).thenReturn(existing);
        when(legalDocumentParsingService.parseAndChunkDocx(file, existing)).thenReturn(List.of(parsedChunk));
        when(lawChunkRepository.findAllByDocumentIdOrderByArticleRefAsc(documentId)).thenReturn(List.of(parsedChunk));

        var response = legalDocumentService.upload(file, dto);

        verify(legalDocumentDiffService).processDocumentUpdate(existing, List.of(parsedChunk));
        verify(lawChunkRepository, never()).saveAll(any());
        verify(documentEmbeddingProcessor).processEmbeddingsForDocument(documentId);
        assertEquals(documentId, response.getDocumentId());
        assertEquals("New title", existing.getTitle());
        assertEquals(1, response.getChunkCount());
    }
}
