package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.LawyerMessageSenderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advokat chat ichidagi bitta xabar.
 * <p>
 * Har bir xabar bitta {@link LawyerChatEntity} ga tegishli.
 * Mijoz yoki advokat yuborishi mumkin — {@link LawyerMessageSenderType} bilan ajratiladi.
 */
@Entity
@Table(
        name = "lawyer_message",
        indexes = {
                @Index(name = "idx_lawyer_message_chat_id_created_date", columnList = "lawyer_chat_id,created_date")
        }
)
@Getter
@Setter
public class LawyerMessageEntity {

    /** Xabar ning noyob identifikatori (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Qaysi chatga tegishli */
    @Column(name = "lawyer_chat_id", nullable = false)
    private UUID lawyerChatId;

    /** Chat entity ga bog'lanish (faqat o'qish uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_chat_id", insertable = false, updatable = false)
    private LawyerChatEntity lawyerChat;

    /** Kim yuborgan: USER (mijoz) yoki LAWYER (advokat) */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private LawyerMessageSenderType senderType;

    /** Xabar matni */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** Xabar qachon yuborilgan */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
