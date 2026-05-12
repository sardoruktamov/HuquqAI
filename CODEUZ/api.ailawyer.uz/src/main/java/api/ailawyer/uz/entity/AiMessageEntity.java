package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.AiMessageSenderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI chat ichidagi bitta message.
 * isEscalation=true bo'lsa frontend "Advokatga bog'lanish" tugmasini ko'rsatishi mumkin.
 */
@Entity
@Table(
        name = "ai_message",
        indexes = {
                @Index(name = "idx_ai_message_chat_id_created_date", columnList = "ai_chat_id,created_date")
        }
)
@Getter
@Setter
public class AiMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ai_chat_id", nullable = false)
    private UUID aiChatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_chat_id", insertable = false, updatable = false)
    private AiChatEntity aiChat;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private AiMessageSenderType senderType;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "is_escalation", nullable = false)
    private Boolean isEscalation = Boolean.FALSE;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}

