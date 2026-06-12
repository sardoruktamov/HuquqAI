package api.ailawyer.uz.service;

import api.ailawyer.uz.ai.AiChatHistoryItem;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * AI chat message logikasi:
 * - User message saqlanadi
 * - Oxirgi 6 ta xabar konteksti Gemini'ga yuboriladi
 * - AI javobi saqlanadi
 * - Trigger so'zlar bo'lsa AI message isEscalation=true bo'ladi
 */
@Service
@RequiredArgsConstructor
public class AiMessageService {

    private static final int HISTORY_LIMIT = 6;

    private static final Pattern ESCALATION_PATTERN = Pattern.compile(
            "advokat|narkotik|qurol|qora dori|nasha|sud|qotillik|huquqshunos|xavf",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
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

        // 2) oxirgi 6 ta xabar tarixini Gemini'ga yuborish
        List<AiChatHistoryItem> history = buildChatHistory(chat.getId());

        AiRequest req = new AiRequest();
        req.setPrompt(dto.getContent());
        req.setHistory(history);
        req.setSystemPromptVersion(AiPromptVersion.V1);
        req.setMetadata(Map.of(
                "aiChatId", chat.getId().toString(),
                "clientId", SpringSecurityUtil.getCurrentUserId()
        ));
        AiResponse res = aiProvider.generate(req);

        if (res == null || res.getText() == null || res.getText().isBlank()) {
            throw new AppBadException("AI javob bermadi!");
        }

        // 3) escalation tekshirish (user xabari va AI javobiga qarab)
        boolean escalation = containsTrigger(userMsg.getContent()) || containsTrigger(res.getText());

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

    private List<AiChatHistoryItem> buildChatHistory(UUID aiChatId) {
        PageRequest pr = PageRequest.of(0, HISTORY_LIMIT);
        Page<AiMessageEntity> page = aiMessageRepository.findAllByAiChatIdOrderByCreatedDateDesc(aiChatId, pr);

        List<AiMessageEntity> messages = new ArrayList<>(page.getContent());
        Collections.reverse(messages);

        List<AiChatHistoryItem> history = new ArrayList<>();
        for (AiMessageEntity message : messages) {
            AiChatHistoryItem item = new AiChatHistoryItem();
            item.setRole(message.getSenderType() == AiMessageSenderType.AI ? "model" : "user");
            item.setContent(message.getContent());
            history.add(item);
        }
        return history;
    }

    private boolean containsTrigger(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return ESCALATION_PATTERN.matcher(text).find();
    }

    private void saveAttachments(UUID messageId, List<String> attachIds) {
        if (attachIds == null || attachIds.isEmpty()) return;

        for (String attachId : attachIds) {
            if (attachId == null || attachId.isBlank()) continue;

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
