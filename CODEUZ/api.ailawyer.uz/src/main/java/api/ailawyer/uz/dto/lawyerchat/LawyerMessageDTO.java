package api.ailawyer.uz.dto.lawyerchat;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.enums.LawyerMessageSenderType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Advokat chat message DTO.
 */
@Getter
@Setter
public class LawyerMessageDTO {
    private UUID id;
    private UUID lawyerChatId;
    private LawyerMessageSenderType senderType;
    private String content;
    private LocalDateTime createdDate;
    private List<AttachDTO> attachments;
}

