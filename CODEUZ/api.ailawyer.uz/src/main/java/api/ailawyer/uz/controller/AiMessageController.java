package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.aichat.AiMessageCreateDTO;
import api.ailawyer.uz.dto.aichat.AiMessageDTO;
import api.ailawyer.uz.service.AiMessageService;
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
 * AI chat ichidagi message endpointlari.
 */
@RestController
@RequestMapping("/api/v1/ai-chats/{aiChatId}/messages")
@Tag(name = "AiMessageController", description = "AI chat message'lari")
@RequiredArgsConstructor
public class AiMessageController {

    private final AiMessageService aiMessageService;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "AI message tarix", description = "AI chat message tarixini olish")
    public ResponseEntity<ApiResponse<PageImpl<AiMessageDTO>>> list(
            @PathVariable("aiChatId") UUID aiChatId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMessageService.list(aiChatId, PageUtil.page(page), size)));
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "AI chatga yozish", description = "User message + AI javob (Gemini) ni saqlash. Escalation flag qaytadi.")
    public ResponseEntity<ApiResponse<AiMessageDTO>> send(
            @PathVariable("aiChatId") UUID aiChatId,
            @Valid @RequestBody AiMessageCreateDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMessageService.sendUserMessage(aiChatId, dto)));
    }
}

