package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.DocumentStatus;
import api.ailawyer.uz.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Huquqiy hujjat metadata (RAG manba hujjati).
 */
@Entity
@Table(
        name = "legal_documents",
        indexes = {
                @Index(name = "idx_legal_documents_doc_number", columnList = "doc_number"),
                @Index(name = "idx_legal_documents_status", columnList = "status"),
                @Index(name = "idx_legal_documents_type", columnList = "type")
        }
)
@Getter
@Setter
public class LegalDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private DocumentType type;

    /** Hujjat raqami, masalan: VMQ-370 */
    @Column(name = "doc_number", nullable = false, length = 64)
    private String docNumber;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    /** Ushbu hujjatni almashtirgan yangi hujjat id si */
    @Column(name = "superseded_by_id")
    private UUID supersededById;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
