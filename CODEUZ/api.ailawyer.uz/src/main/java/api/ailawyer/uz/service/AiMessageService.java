package api.ailawyer.uz.service;

import api.ailawyer.uz.ai.AiProvider;
import api.ailawyer.uz.ai.AiPromptVersion;
import api.ailawyer.uz.ai.AiRequest;
import api.ailawyer.uz.ai.AiResponse;
import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.dto.aichat.AiMessageCreateDTO;
import api.ailawyer.uz.dto.aichat.AiMessageDTO;
import api.ailawyer.uz.entity.AiChatEntity;
import api.ailawyer.uz.entity.AiMessageAttachEntity;
import api.ailawyer.uz.entity.AiMessageEntity;
import api.ailawyer.uz.enums.AiMessageSenderType;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.AiMessageAttachRepository;
import api.ailawyer.uz.repository.AiMessageRepository;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI chat message logikasi:
 * - User message saqlanadi
 * - AI provider (Gemini)ga yuboriladi
 * - AI javobi saqlanadi
 * - Trigger so'zlar bo'lsa AI message isEscalation=true bo'ladi
 */
@Service
@RequiredArgsConstructor
public class AiMessageService {

    private static final List<String> ESCALATION_TRIGGERS = List.of(
            "advokat", "sud", "jinoiy", "huquqshunos", "xavf", "sudya"
    );

    private final AiChatService aiChatService;
    private final AiProvider aiProvider;
    private final AiMessageRepository aiMessageRepository;
    private final AiMessageAttachRepository aiMessageAttachRepository;
    private final AttachService attachService;

    public PageImpl<AiMessageDTO> list(UUID aiChatId, int page, int size) {
        AiChatEntity chat = aiChatService.getEntityForRead(aiChatId);

        PageRequest pr = PageRequest.of(page, size);
        Page<AiMessageEntity> p = aiMessageRepository.findAllByAiChatIdOrderByCreatedDateDesc(chat.getId(), pr);

        List<AiMessageEntity> messages = p.getContent();
        Map<UUID, List<AttachDTO>> attachments = getAttachmentMap(messages);

        List<AiMessageDTO> list = messages.stream()
                .map(m -> toDto(m, attachments.getOrDefault(m.getId(), List.of())))
                .toList();

        return new PageImpl<>(list, pr, p.getTotalElements());
    }

    public AiMessageDTO sendUserMessage(UUID aiChatId, AiMessageCreateDTO dto) {
        AiChatEntity chat = aiChatService.getEntityForWrite(aiChatId);

        // 1) user message saqlash
        AiMessageEntity userMsg = new AiMessageEntity();
        userMsg.setAiChatId(chat.getId());
        userMsg.setSenderType(AiMessageSenderType.USER);
        userMsg.setContent(dto.getContent());
        userMsg.setIsEscalation(false);
        userMsg.setCreatedDate(LocalDateTime.now());
        aiMessageRepository.save(userMsg);
        saveAttachments(userMsg.getId(), dto.getAttachIds());

        // 2) AI provider (Gemini)ga yuborish
        AiRequest req = new AiRequest();
        req.setPrompt(dto.getContent());
        req.setSystemPromptVersion(AiPromptVersion.V1);
        req.setMetadata(Map.of(
                "aiChatId", chat.getId().toString(),
                "clientId", SpringSecurityUtil.getCurrentUserId()
        ));
        AiResponse res = aiProvider.generate(req);

        if (res == null || res.getText() == null || res.getText().isBlank()) {
            throw new AppBadException("AI javob bermadi!");
        }

        // 3) escalation tekshirish (AI javobiga qarab)
        boolean escalation = containsTrigger(res.getText());

        // 4) AI message saqlash
        AiMessageEntity aiMsg = new AiMessageEntity();
        aiMsg.setAiChatId(chat.getId());
        aiMsg.setSenderType(AiMessageSenderType.AI);
        aiMsg.setContent(res.getText());
        aiMsg.setIsEscalation(escalation);
        aiMsg.setCreatedDate(LocalDateTime.now());
        aiMessageRepository.save(aiMsg);

        return toDto(aiMsg, List.of());
    }

    private boolean containsTrigger(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String t : ESCALATION_TRIGGERS) {
            if (lower.contains(t)) return true;
        }
        return false;
    }

    private void saveAttachments(UUID messageId, List<String> attachIds) {
        if (attachIds == null || attachIds.isEmpty()) return;

        for (String attachId : attachIds) {
            if (attachId == null || attachId.isBlank()) continue;

            // mavjudligini tekshiradi (topilmasa exception)
            attachService.getEntity(attachId);

            AiMessageAttachEntity link = new AiMessageAttachEntity();
            link.setMessageId(messageId);
            link.setAttachId(attachId);
            link.setCreatedDate(LocalDateTime.now());
            aiMessageAttachRepository.save(link);
        }
    }

    private Map<UUID, List<AttachDTO>> getAttachmentMap(List<AiMessageEntity> messages) {
        if (messages.isEmpty()) return Map.of();

        List<UUID> ids = messages.stream().map(AiMessageEntity::getId).toList();
        List<AiMessageAttachEntity> links = aiMessageAttachRepository.findAllByMessageIdIn(ids);

        Map<UUID, List<AttachDTO>> map = new HashMap<>();
        for (AiMessageAttachEntity l : links) {
            map.computeIfAbsent(l.getMessageId(), k -> new ArrayList<>())
                    .add(attachService.attachDTO(l.getAttachId()));
        }
        return map;
    }

    private AiMessageDTO toDto(AiMessageEntity e, List<AttachDTO> attachments) {
        AiMessageDTO dto = new AiMessageDTO();
        dto.setId(e.getId());
        dto.setAiChatId(e.getAiChatId());
        dto.setSenderType(e.getSenderType());
        dto.setContent(e.getContent());
        dto.setIsEscalation(Boolean.TRUE.equals(e.getIsEscalation()));
        dto.setCreatedDate(e.getCreatedDate());
        dto.setAttachments(attachments);
        return dto;
    }
}

