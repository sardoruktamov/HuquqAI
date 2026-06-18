package api.ailawyer.uz.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tizimdagi muhim harakatlar tarixi (Level 2 audit log).
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_entity", columnList = "entity_name, entity_id"),
                @Index(name = "idx_audit_logs_performed_by", columnList = "performed_by"),
                @Index(name = "idx_audit_logs_performed_at", columnList = "performed_at")
        }
)
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** O'zgartirilgan jadval nomi, masalan: lawyer_profile */
    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    /** O'zgartirilgan obyekt id si */
    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    /** Bajarilgan harakat, masalan: APPROVE, REJECT */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    /** Harakatni bajargan foydalanuvchi profile id si */
    @Column(name = "performed_by", nullable = false)
    private Integer performedBy;

    /** Harakat bajarilgan vaqt */
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    /** Qo'shimcha ma'lumot (masalan, rad etish sababi) */
    @Column(name = "details", columnDefinition = "text")
    private String details;
}
