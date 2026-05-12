package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.aichat.AiChatCreateDTO;
import api.ailawyer.uz.dto.aichat.AiChatDTO;
import api.ailawyer.uz.service.AiChatService;
import api.ailawyer.uz.util.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AI chatlar uchun REST API.
 */
@RestController
@RequestMapping("/api/v1/ai-chats")
@Tag(name = "AiChatController", description = "AI chatlar (izolyatsiya qilingan)")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "AI chat yaratish", description = "Client yangi AI chat ochadi")
    public ResponseEntity<ApiResponse<AiChatDTO>> create(@Valid @RequestBody AiChatCreateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(aiChatService.create(dto)));
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "AI chatlar ro'yxati", description = "Client o'z AI chatlarini oladi")
    public ResponseEntity<ApiResponse<PageImpl<AiChatDTO>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiChatService.getMyChats(PageUtil.page(page), size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "AI chat detail", description = "AI chat detail")
    public ResponseEntity<ApiResponse<AiChatDTO>> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(aiChatService.getById(id)));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "AI chat yopish", description = "AI chat'ni yopish (CLOSED)")
    public ResponseEntity<ApiResponse<String>> close(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(aiChatService.close(id)));
    }
}

