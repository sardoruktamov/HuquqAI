package api.ailawyer.uz.dto.lawyerchat;

import api.ailawyer.uz.enums.LawyerChatStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advokat chat ro'yxati/detail uchun DTO.
 */
@Getter
@Setter
public class LawyerChatDTO {
    private UUID id;
    private Integer clientId;
    private Integer lawyerId;
    private LawyerChatStatus status;
    private LocalDateTime createdDate;
}

