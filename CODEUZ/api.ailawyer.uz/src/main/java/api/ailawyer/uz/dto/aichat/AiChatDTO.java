package api.ailawyer.uz.dto.aichat;

import api.ailawyer.uz.enums.AiChatStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI chat ro'yxati/detail uchun DTO.
 */
@Getter
@Setter
public class AiChatDTO {
    private UUID id;
    private Integer clientId;
    private String title;
    private AiChatStatus status;
    private LocalDateTime createdDate;
}

