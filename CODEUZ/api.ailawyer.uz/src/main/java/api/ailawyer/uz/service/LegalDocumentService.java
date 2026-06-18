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

@Service
@RequiredArgsConstructor
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final LawChunkRepository lawChunkRepository;
    private final LegalDocumentParsingService legalDocumentParsingService;

    @Transactional
    public LegalDocumentUploadResponseDTO upload(MultipartFile file, LegalDocumentUploadDTO dto) {
        if (legalDocumentRepository.findByDocNumber(dto.getDocNumber().trim()).isPresent()) {
            throw new AppBadException("Bu hujjat raqami allaqachon mavjud: " + dto.getDocNumber());
        }

        if (dto.getSupersededById() != null
                && !legalDocumentRepository.existsById(dto.getSupersededById())) {
            throw new AppBadException("supersededById bo'yicha hujjat topilmadi!");
        }

        LegalDocumentEntity document = new LegalDocumentEntity();
        document.setType(dto.getType());
        document.setDocNumber(dto.getDocNumber().trim());
        document.setDocDate(dto.getDocDate());
        document.setTitle(dto.getTitle().trim());
        document.setStatus(DocumentStatus.ACTIVE);
        document.setSupersededById(dto.getSupersededById());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        document = legalDocumentRepository.save(document);

        List<LawChunkEntity> chunks = legalDocumentParsingService.parseAndChunkDocx(file, document);
        lawChunkRepository.saveAll(chunks);

        LegalDocumentUploadResponseDTO response = new LegalDocumentUploadResponseDTO();
        response.setDocumentId(document.getId());
        response.setType(document.getType());
        response.setDocNumber(document.getDocNumber());
        response.setDocDate(document.getDocDate());
        response.setTitle(document.getTitle());
        response.setStatus(document.getStatus());
        response.setChunkCount(chunks.size());
        return response;
    }
}
