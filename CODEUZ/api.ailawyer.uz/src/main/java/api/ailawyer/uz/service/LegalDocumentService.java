package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.legal.LegalDocumentUploadDTO;
import api.ailawyer.uz.dto.legal.LegalDocumentUploadResponseDTO;
import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentStatus;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawChunkRepository;
import api.ailawyer.uz.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final LawChunkRepository lawChunkRepository;
    private final LegalDocumentParsingService legalDocumentParsingService;
    private final LegalDocumentDiffService legalDocumentDiffService;

    @Transactional
    public LegalDocumentUploadResponseDTO upload(MultipartFile file, LegalDocumentUploadDTO dto) {
        validateSupersededById(dto.getSupersededById());

        String docNumber = dto.getDocNumber().trim();
        Optional<LegalDocumentEntity> existingDocument = legalDocumentRepository.findByDocNumber(docNumber);

        if (existingDocument.isPresent()) {
            return updateExistingDocument(file, dto, existingDocument.get());
        }

        return createNewDocument(file, dto, docNumber);
    }

    private LegalDocumentUploadResponseDTO createNewDocument(
            MultipartFile file,
            LegalDocumentUploadDTO dto,
            String docNumber
    ) {
        LegalDocumentEntity document = new LegalDocumentEntity();
        document.setType(dto.getType());
        document.setDocNumber(docNumber);
        document.setDocDate(dto.getDocDate());
        document.setTitle(dto.getTitle().trim());
        document.setStatus(DocumentStatus.ACTIVE);
        document.setSupersededById(dto.getSupersededById());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        document = legalDocumentRepository.save(document);

        List<LawChunkEntity> chunks = legalDocumentParsingService.parseAndChunkDocx(file, document);
        lawChunkRepository.saveAll(chunks);

        return buildResponse(document, chunks.size());
    }

    private LegalDocumentUploadResponseDTO updateExistingDocument(
            MultipartFile file,
            LegalDocumentUploadDTO dto,
            LegalDocumentEntity existingDocument
    ) {
        applyMetadataUpdate(existingDocument, dto);

        List<LawChunkEntity> newlyParsedChunks =
                legalDocumentParsingService.parseAndChunkDocx(file, existingDocument);

        legalDocumentDiffService.processDocumentUpdate(existingDocument, newlyParsedChunks);

        int totalChunks = lawChunkRepository.findAllByDocumentIdOrderByArticleRefAsc(existingDocument.getId()).size();
        return buildResponse(existingDocument, totalChunks);
    }

    private void applyMetadataUpdate(LegalDocumentEntity document, LegalDocumentUploadDTO dto) {
        document.setType(dto.getType());
        document.setTitle(dto.getTitle().trim());
        document.setDocDate(dto.getDocDate());
        if (dto.getSupersededById() != null) {
            document.setSupersededById(dto.getSupersededById());
        }
        document.setUpdatedAt(LocalDateTime.now());
        legalDocumentRepository.save(document);
    }

    private void validateSupersededById(UUID supersededById) {
        if (supersededById != null && !legalDocumentRepository.existsById(supersededById)) {
            throw new AppBadException("supersededById bo'yicha hujjat topilmadi!");
        }
    }

    private LegalDocumentUploadResponseDTO buildResponse(LegalDocumentEntity document, int chunkCount) {
        LegalDocumentUploadResponseDTO response = new LegalDocumentUploadResponseDTO();
        response.setDocumentId(document.getId());
        response.setType(document.getType());
        response.setDocNumber(document.getDocNumber());
        response.setDocDate(document.getDocDate());
        response.setTitle(document.getTitle());
        response.setStatus(document.getStatus());
        response.setChunkCount(chunkCount);
        return response;
    }
}
