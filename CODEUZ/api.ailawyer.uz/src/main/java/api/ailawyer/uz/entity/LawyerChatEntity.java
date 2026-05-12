package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.LawyerChatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mijoz va advokat o'rtasidagi chat (izolyatsiya qilingan).
 * Bu chat AI chat tarixidan mustaqil, noldan boshlanadi.
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private ProfileEntity client;

    @Column(name = "lawyer_id", nullable = false)
    private Integer lawyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", insertable = false, updatable = false)
    private ProfileEntity lawyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LawyerChatStatus status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}

