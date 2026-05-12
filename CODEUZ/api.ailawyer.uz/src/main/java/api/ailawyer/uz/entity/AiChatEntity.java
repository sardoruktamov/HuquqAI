package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.AiChatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI bilan chat (izolyatsiya qilingan).
 * Bu chat tarixi advokat chatiga hech qachon o'tmaydi.
 */
@Entity
@Table(
        name = "ai_chat",
        indexes = {
                @Index(name = "idx_ai_chat_client_id", columnList = "client_id"),
                @Index(name = "idx_ai_chat_created_date", columnList = "created_date")
        }
)
@Getter
@Setter
public class AiChatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private ProfileEntity client;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiChatStatus status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}

