package api.ailawyer.uz.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI message va attach o'rtasidagi bog'lanish (ko'prik).
 */
@Entity
@Table(
        name = "ai_message_attach",
        indexes = {
                @Index(name = "idx_ai_message_attach_message_id", columnList = "message_id"),
                @Index(name = "idx_ai_message_attach_attach_id", columnList = "attach_id")
        }
)
@Getter
@Setter
public class AiMessageAttachEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private AiMessageEntity message;

    @Column(name = "attach_id", nullable = false)
    private String attachId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attach_id", insertable = false, updatable = false)
    private AttachEntity attach;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}

