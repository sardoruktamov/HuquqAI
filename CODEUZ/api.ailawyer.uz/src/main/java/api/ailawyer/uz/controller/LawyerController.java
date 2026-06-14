package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.lawyer.*;
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
 * Advokatlar tizimi REST API controlleri.
 * <p>
 * Ikki guruh endpoint:
 * <ul>
 *   <li>Advokat o'zi — onboarding, profil yangilash, submit</li>
 *   <li>Barcha foydalanuvchilar — tasdiqlangan advokatlar katalogi va detail</li>
 * </ul>
 * Admin endpointlar {@link LawyerAdminController} da.
 */
@RestController
@RequestMapping("/api/v1/lawyers")
@Tag(name = "LawyerController", description = "Advokatlar tizimi (katalog, profil, onboarding)")
@RequiredArgsConstructor
public class LawyerController {

    /** Advokat profili biznes logikasi */
    private final LawyerProfileService lawyerProfileService;

    /**
     * POST /api/v1/lawyers/onboarding
     * <p>
     * Oddiy foydalanuvchi advokat bo'lish uchun ma'lumotlarini kiritadi.
     * Avtomatik ROLE_LAWYER beriladi, holat DRAFT bo'ladi.
     *
     * @param dto ixtisoslik, tajriba, litsenziya, hudud va hokazo
     */
    @PostMapping("/onboarding")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokat onboarding", description = "Advokat profil ma'lumotlarini yaratish/yangilash (DRAFT)")
    public ResponseEntity<ApiResponse<LawyerProfileDTO>> onboarding(@Valid @RequestBody LawyerOnboardingDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.onboarding(dto)));
    }

    /**
     * PUT /api/v1/lawyers/profile
     * <p>
     * Faqat advokat roli bor foydalanuvchi o'z profilini yangilaydi.
     *
     * @param dto yangilanadigan maydonlar
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokat profilini yangilash", description = "Tasdiqlangan advokat o'z profilini yangilaydi")
    public ResponseEntity<ApiResponse<LawyerProfileDTO>> updateProfile(@Valid @RequestBody LawyerProfileUpdateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.updateProfile(dto)));
    }

    /**
     * GET /api/v1/lawyers/me
     * <p>
     * Joriy kirgan advokat o'z profilini ko'radi (onboarding holati, rad sababi va hokazo).
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Mening advokat profilim", description = "Joriy advokat o'z profilini ko'radi")
    public ResponseEntity<ApiResponse<LawyerProfileDTO>> me() {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.getMe()));
    }

    /**
     * POST /api/v1/lawyers/onboarding/submit
     * <p>
     * Advokat profilni admin tasdiqlashiga yuboradi (PENDING holat).
     */
    @PostMapping("/onboarding/submit")
    @PreAuthorize("hasAnyRole('ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Onboarding yuborish", description = "Profilni admin tasdiqlashiga yuborish (PENDING)")
    public ResponseEntity<ApiResponse<LawyerProfileDTO>> submitOnboarding() {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.submitOnboarding()));
    }

    /**
     * GET /api/v1/lawyers/public
     * <p>
     * Tasdiqlangan advokatlar ro'yxati — query parametrlar orqali filter.
     * Litsenziya ma'lumotlari ko'rsatilmaydi.
     *
     * @param query           ism, ixtisoslik yoki hudud bo'yicha qidiruv
     * @param region          hudud filteri
     * @param specialization  ixtisoslik filteri
     * @param minExperience   minimal tajriba yili
     * @param page            sahifa (1 dan boshlanadi)
     * @param size            sahifa hajmi
     */
    @GetMapping("/public")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokatlar ro'yxati", description = "Tasdiqlangan advokatlar katalogi (oddiy foydalanuvchilar uchun)")
    public ResponseEntity<ApiResponse<PageImpl<LawyerPublicDTO>>> publicList(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "specialization", required = false) String specialization,
            @RequestParam(value = "minExperience", required = false) Integer minExperience,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        LawyerFilterDTO filter = new LawyerFilterDTO();
        filter.setQuery(query);
        filter.setRegion(region);
        filter.setSpecialization(specialization);
        filter.setMinExperience(minExperience);
        return ResponseEntity.ok(ApiResponse.success(
                lawyerProfileService.publicFilter(filter, PageUtil.page(page), size)));
    }

    /**
     * POST /api/v1/lawyers/public/filter
     * <p>
     * Xuddi public ro'yxat, lekin filter body (JSON) orqali yuboriladi.
     *
     * @param dto  filter obyekti (bo'sh bo'lishi mumkin)
     * @param page sahifa raqami
     * @param size sahifa hajmi
     */
    @PostMapping("/public/filter")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokatlar filter", description = "Tasdiqlangan advokatlar katalogi (filter body bilan)")
    public ResponseEntity<ApiResponse<PageImpl<LawyerPublicDTO>>> publicFilter(
            @RequestBody(required = false) LawyerFilterDTO dto,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        if (dto == null) {
            dto = new LawyerFilterDTO();
        }
        return ResponseEntity.ok(ApiResponse.success(
                lawyerProfileService.publicFilter(dto, PageUtil.page(page), size)));
    }

    /**
     * GET /api/v1/lawyers/public/{id}
     * <p>
     * Bitta advokatning to'liq profili — bio, litsenziya raqami va hujjat URL bilan.
     *
     * @param id advokat profile id si
     */
    @GetMapping("/public/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Advokat profili", description = "Advokat detail + litsenziya ma'lumotlari (oddiy foydalanuvchilar uchun)")
    public ResponseEntity<ApiResponse<LawyerPublicDetailDTO>> publicDetail(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(ApiResponse.success(lawyerProfileService.publicGetById(id)));
    }
}
