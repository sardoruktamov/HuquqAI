package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.lawyerchat.LawyerChatStartDTO;
import api.ailawyer.uz.dto.lawyerchat.LawyerMessageCreateDTO;
import api.ailawyer.uz.dto.lawyerchat.LawyerMessageDTO;
import api.ailawyer.uz.service.LawyerMessageService;
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
 * Lawyer chat ichidagi message endpointlari.
 */
@RestController
@Tag(name = "LawyerMessageController", description = "Advokat chat message'lari")
@RequiredArgsConstructor
public class LawyerMessageController {

    private final LawyerMessageService lawyerMessageService;

    @PostMapping("/api/v1/lawyer-chats/start")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Chat boshlash + birinchi message", description = "Client advokatga birinchi message yuboradi, chat ACTIVE yaratiladi")
    public ResponseEntity<ApiResponse<LawyerMessageDTO>> start(@Valid @RequestBody LawyerChatStartDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(lawyerMessageService.startChatAndSend(dto)));
    }

    @GetMapping("/api/v1/lawyer-chats/{lawyerChatId}/messages")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Lawyer message tarix", description = "Lawyer chat message tarixini olish")
    public ResponseEntity<ApiResponse<PageImpl<LawyerMessageDTO>>> list(
            @PathVariable("lawyerChatId") UUID lawyerChatId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(lawyerMessageService.list(lawyerChatId, PageUtil.page(page), size)));
    }

    @PostMapping("/api/v1/lawyer-chats/{lawyerChatId}/messages")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Lawyer chatga yozish", description = "Client yoki lawyer message yuboradi")
    public ResponseEntity<ApiResponse<LawyerMessageDTO>> send(
            @PathVariable("lawyerChatId") UUID lawyerChatId,
            @Valid @RequestBody LawyerMessageCreateDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(lawyerMessageService.sendMessage(lawyerChatId, dto)));
    }
}

