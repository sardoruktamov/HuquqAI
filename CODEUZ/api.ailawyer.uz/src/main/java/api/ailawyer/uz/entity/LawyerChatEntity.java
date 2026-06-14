package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.LawyerChatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mijoz va advokat o'rtasidagi alohida chat.
 * <p>
 * AI chat tarixidan 100% izolyatsiya qilingan — advokat AI suhbatini ko'rmaydi.
 * Mijoz advokat profilidan birinchi xabar yozganda ACTIVE holatda yaratiladi.
 */
@Entity
@Table(
        name = "lawyer_chat",
        indexes = {
                @Index(name = "idx_lawyer_chat_client_id", columnList = "client_id"),
                @Index(name = "idx_lawyer_chat_lawyer_id", columnList = "lawyer_id"),
                @Index(name = "idx_lawyer_chat_created_date", columnList = "created_date")
        }
)
@Getter
@Setter
public class LawyerChatEntity {

    /** Chat ning noyob identifikatori (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Mijoz (oddiy foydalanuvchi) profile id si */
    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    /** Mijoz profiliga bog'lanish (faqat o'qish uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private ProfileEntity client;

    /** Advokat profile id si */
    @Column(name = "lawyer_id", nullable = false)
    private Integer lawyerId;

    /** Advokat profiliga bog'lanish (faqat o'qish uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", insertable = false, updatable = false)
    private ProfileEntity lawyer;

    /** Chat holati: ACTIVE yoki CLOSED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LawyerChatStatus status;

    /** Chat qachon yaratilgan */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
