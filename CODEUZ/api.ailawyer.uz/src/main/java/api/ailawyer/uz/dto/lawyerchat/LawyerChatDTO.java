package api.ailawyer.uz.dto.lawyerchat;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.enums.LawyerChatStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advokat chat ro'yxati va detail uchun javob DTO.
 * <p>
 * GET /api/v1/lawyer-chats va GET /api/v1/lawyer-chats/{id} da qaytariladi.
 * Mobil ilovada chat ro'yxatida ism, rasm va oxirgi xabar preview ko'rsatish uchun.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LawyerChatDTO {

    /** Chat UUID si */
    private UUID id;

    /** Mijoz profile id si */
    private Integer clientId;

    /** Advokat profile id si */
    private Integer lawyerId;

    /** Chat holati: ACTIVE yoki CLOSED */
    private LawyerChatStatus status;

    /** Chat yaratilgan vaqt */
    private LocalDateTime createdDate;

    /** Mijoz to'liq ismi (mobil ro'yxat uchun) */
    private String clientName;

    /** Mijoz profil rasmi URL bilan */
    private AttachDTO clientPhoto;

    /** Advokat to'liq ismi (mobil ro'yxat uchun) */
    private String lawyerName;

    /** Advokat profil rasmi URL bilan */
    private AttachDTO lawyerPhoto;

    /** Oxirgi yuborilgan xabar matni (preview) */
    private String lastMessageContent;

    /** Oxirgi xabar vaqti */
    private LocalDateTime lastMessageDate;

    /** O'qilmagan xabarlar soni (hozircha 0, keyin FCM bilan to'ldiriladi) */
    private Long unreadCount;
}
