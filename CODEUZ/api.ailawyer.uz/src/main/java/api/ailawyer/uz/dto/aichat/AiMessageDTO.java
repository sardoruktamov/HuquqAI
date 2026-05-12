package api.ailawyer.uz.dto.aichat;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.enums.AiMessageSenderType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI chat message DTO.
 */
@Getter
@Setter
public class AiMessageDTO {
    private UUID id;
    private UUID aiChatId;
    private AiMessageSenderType senderType;
    private String content;
    private Boolean isEscalation;
    private LocalDateTime createdDate;
    private List<AttachDTO> attachments;
}

