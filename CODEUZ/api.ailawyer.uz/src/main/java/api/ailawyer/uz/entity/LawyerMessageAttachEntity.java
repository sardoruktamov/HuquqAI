package api.ailawyer.uz.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advokat chat xabariga biriktirilgan fayl (rasm, PDF va hokazo).
 * <p>
 * Bir xabarga bir nechta fayl biriktirish mumkin — har biri alohida qator.
 * Fayl o'zi {@link AttachEntity} jadvalida saqlanadi, bu jadval faqat bog'lanish.
 */
@Entity
@Table(
        name = "lawyer_message_attach",
        indexes = {
                @Index(name = "idx_lawyer_message_attach_message_id", columnList = "message_id"),
                @Index(name = "idx_lawyer_message_attach_attach_id", columnList = "attach_id")
        }
)
@Getter
@Setter
public class LawyerMessageAttachEntity {

    /** Bog'lanish qatorining id si */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Qaysi xabarga fayl biriktirilgan */
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    /** Xabar entity ga bog'lanish (faqat o'qish uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private LawyerMessageEntity message;

    /** Yuklangan fayl id si (attach jadvalidagi id) */
    @Column(name = "attach_id", nullable = false)
    private String attachId;

    /** Fayl entity ga bog'lanish (faqat o'qish uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attach_id", insertable = false, updatable = false)
    private AttachEntity attach;

    /** Fayl qachon biriktirilgan */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
