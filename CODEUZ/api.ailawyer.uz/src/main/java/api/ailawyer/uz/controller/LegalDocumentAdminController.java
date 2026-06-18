package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.legal.LegalDocumentUploadDTO;
import api.ailawyer.uz.dto.legal.LegalDocumentUploadResponseDTO;
import api.ailawyer.uz.service.LegalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin uchun huquqiy hujjatlar yuklash va boshqarish.
 */
@RestController
@RequestMapping("/api/v1/admin/legal-documents")
@Tag(name = "LegalDocumentAdminController", description = "Huquqiy hujjatlar admin boshqaruvi (RAG)")
@RequiredArgsConstructor
public class LegalDocumentAdminController {

    private final LegalDocumentService legalDocumentService;

    /**
     * POST /api/v1/admin/legal-documents/upload
     * <p>
     * .docx hujjatni yuklaydi, modda/band bo'laklariga ajratadi va bazaga saqlaydi.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Huquqiy hujjat yuklash", description = ".docx faylni parse qilib law_chunks jadvaliga saqlaydi")
    public ResponseEntity<ApiResponse<LegalDocumentUploadResponseDTO>> upload(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute LegalDocumentUploadDTO dto
    ) {
        LegalDocumentUploadResponseDTO response = legalDocumentService.upload(file, dto);
        return ResponseEntity.ok(ApiResponse.success(
                "Hujjat muvaffaqiyatli yuklandi, chunkCount=" + response.getChunkCount(),
                response
        ));
    }
}
