package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.LawyerMessageSenderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Advokat chat ichidagi bitta message.
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lawyer_chat_id", nullable = false)
    private UUID lawyerChatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_chat_id", insertable = false, updatable = false)
    private LawyerChatEntity lawyerChat;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private LawyerMessageSenderType senderType;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}

