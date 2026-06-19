package api.ailawyer.uz.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * RAG uchun PostgreSQL pgvector optimizatsiyasi (HNSW indeks).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagDatabaseConfig {

    private static final String HNSW_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS law_chunks_embedding_hnsw_idx "
                    + "ON law_chunks USING hnsw (embedding vector_cosine_ops)";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureHnswIndex() {
        try {
            jdbcTemplate.execute(HNSW_INDEX_SQL);
            log.info("HNSW indeks tayyor: law_chunks_embedding_hnsw_idx");
        } catch (Exception e) {
            log.warn("HNSW indeks yaratib bo'lmadi (pgvector yo'q yoki jadval hali mavjud emas): {}", e.getMessage());
        }
    }
}
