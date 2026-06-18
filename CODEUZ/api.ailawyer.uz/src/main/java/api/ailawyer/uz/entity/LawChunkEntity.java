package api.ailawyer.uz.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Huquqiy hujjatning modda bo'lagi va uning vektor embeddingi (RAG).
 */
@Entity
@Table(
        name = "law_chunks",
        indexes = {
                @Index(name = "idx_law_chunks_document_id", columnList = "document_id"),
                @Index(name = "idx_law_chunks_article_ref", columnList = "article_ref"),
                @Index(name = "idx_law_chunks_text_hash", columnList = "text_hash")
        }
)
@Getter
@Setter
public class LawChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private LegalDocumentEntity document;

    /** Modda havolasi, masalan: 12-modda */
    @Column(name = "article_ref", nullable = false, length = 64)
    private String articleRef;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** content maydonining SHA-256 hash qiymati (aqlli yangilash uchun) */
    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    /** Gemini text-embedding-004 vektori (768 o'lcham) */
    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private float[] embedding;
}
