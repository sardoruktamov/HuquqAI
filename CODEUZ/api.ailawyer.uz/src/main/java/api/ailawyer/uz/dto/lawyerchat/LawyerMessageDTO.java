package api.ailawyer.uz.dto.lawyerchat;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.enums.LawyerMessageSenderType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Advokat chatidagi bitta xabar javob DTO.
 * <p>
 * Xabar yuborish va tarix olish endpointlarida qaytariladi.
 */
@Getter
@Setter
public class LawyerMessageDTO {

    /** Xabar UUID si */
    private UUID id;

    /** Qaysi chatga tegishli */
    private UUID lawyerChatId;

    /** Kim yuborgan: USER (mijoz) yoki LAWYER (advokat) */
    private LawyerMessageSenderType senderType;

    /** Xabar matni */
    private String content;

    /** Xabar yuborilgan vaqt */
    private LocalDateTime createdDate;

    /** Xabarga biriktirilgan fayllar (rasm, PDF va hokazo) */
    private List<AttachDTO> attachments;
}
