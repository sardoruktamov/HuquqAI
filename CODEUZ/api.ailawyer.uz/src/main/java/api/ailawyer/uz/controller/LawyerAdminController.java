package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.lawyer.LawyerProfileDTO;
import api.ailawyer.uz.dto.lawyer.LawyerRejectDTO;
import api.ailawyer.uz.service.LawyerProfileService;
import api.ailawyer.uz.util.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin uchun advokat onboarding boshqaruvi.
 * <p>
 * Faqat ROLE_ADMIN va ROLE_SUPERADMIN kirishi mumkin.
 * Advokat arizalarini ko'rish, tasdiqlash va rad etish.
 */
@RestController
@RequestMapping("/api/v1/admin/lawyers")
@Tag(name = "LawyerAdminController", description = "Advokat onboarding admin boshqaruvi")
@RequiredArgsConstructor
public class LawyerAdminController {

    /** Advokat profili biznes logikasi */
    private final LawyerProfileService lawyerProfileService;

    /**
     * GET /api/v1/admin/lawyers/pending
     * <p>
     * Admin tasdiqlashini kutayotgan (PENDING) advokat arizalari ro'yxati.
     *
     * @param page sahifa raqami (1 dan)
     * @param size sahifa hajmi
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Tasdiqlash kutilayotgan advokatlar", description = "PENDING holatdagi profillar ro'yxati")
    public ResponseEntity<ApiResponse<PageImpl<LawyerProfileDTO>>> pending(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.listPending(PageUtil.page(page), size)));
    }

    /**
     * PUT /api/v1/admin/lawyers/{id}/approve
     * <p>
     * Admin advokat arizasini tasdiqlaydi — holat APPROVED bo'ladi,
     * advokat public katalogda paydo bo'ladi.
     *
     * @param id tasdiqlanadigan advokat profile id si
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokatni tasdiqlash", description = "Onboarding profilini APPROVED qilish")
    public ResponseEntity<ApiResponse<LawyerProfileDTO>> approve(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.approve(id)));
    }

    /**
     * PUT /api/v1/admin/lawyers/{id}/reject
     * <p>
     * Admin advokat arizasini rad etadi — holat REJECTED, sabab body da yuboriladi.
     *
     * @param id  rad etiladigan advokat profile id si
     * @param dto rad etish sababi (reason majburiy)
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokatni rad etish", description = "Onboarding profilini REJECTED qilish")
    public ResponseEntity<ApiResponse<LawyerProfileDTO>> reject(
            @PathVariable("id") Integer id,
            @Valid @RequestBody LawyerRejectDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.reject(id, dto)));
    }
}
