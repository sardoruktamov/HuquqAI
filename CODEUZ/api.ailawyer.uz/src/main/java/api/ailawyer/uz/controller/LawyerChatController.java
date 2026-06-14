package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.lawyerchat.LawyerChatDTO;
import api.ailawyer.uz.service.LawyerChatService;
import api.ailawyer.uz.util.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Advokat chatlari REST API controlleri.
 * <p>
 * Chat ro'yxati, detail va yopish endpointlari.
 * Xabar yuborish {@link LawyerMessageController} da.
 */
@RestController
@RequestMapping("/api/v1/lawyer-chats")
@Tag(name = "LawyerChatController", description = "Advokat chatlar (izolyatsiya qilingan)")
@RequiredArgsConstructor
public class LawyerChatController {

    /** Chat biznes logikasi */
    private final LawyerChatService lawyerChatService;

    /**
     * GET /api/v1/lawyer-chats
     * <p>
     * Joriy foydalanuvchining chatlar ro'yxati.
     * Mijoz o'z chatlarini, advokat o'ziga kelgan chatlarni ko'radi.
     * Har bir chatda ism, rasm va oxirgi xabar preview bor.
     *
     * @param page sahifa (1 dan boshlanadi)
     * @param size sahifa hajmi
     */
    @GetMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Lawyer chatlar ro'yxati", description = "Client o'z chatlari yoki lawyer o'z chatlarini oladi")
    public ResponseEntity<ApiResponse<PageImpl<LawyerChatDTO>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(lawyerChatService.getMyChats(PageUtil.page(page), size)));
    }

    /**
     * GET /api/v1/lawyer-chats/{id}
     * <p>
     * Bitta chatning to'liq ma'lumoti — ishtirokchilar, holat, oxirgi xabar.
     *
     * @param id chat UUID si
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Lawyer chat detail", description = "Chat detail")
    public ResponseEntity<ApiResponse<LawyerChatDTO>> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(lawyerChatService.getById(id)));
    }

    /**
     * PUT /api/v1/lawyer-chats/{id}/close
     * <p>
     * Mijoz yoki advokat chatni yopadi — holat CLOSED bo'ladi.
     * Yopilgan chatga yangi xabar yozib bo'lmaydi.
     *
     * @param id yopiladigan chat UUID si
     */
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Lawyer chat yopish", description = "Chat holatini CLOSED qilish")
    public ResponseEntity<ApiResponse<String>> close(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(lawyerChatService.close(id)));
    }
}
