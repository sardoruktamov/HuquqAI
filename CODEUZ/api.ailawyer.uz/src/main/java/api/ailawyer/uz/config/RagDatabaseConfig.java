package api.ailawyer.uz.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * RAG uchun PostgreSQL pgvector optimizatsiyasi (HNSW indeks).
 * Hibernate {@code law_chunks} jadvalini yaratgandan keyin ishga tushadi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagDatabaseConfig {

    private static final String HNSW_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS law_chunks_embedding_hnsw_idx "
                    + "ON law_chunks USING hnsw (embedding vector_cosine_ops)";

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureHnswIndex() {
        try {
            jdbcTemplate.execute(HNSW_INDEX_SQL);
            log.info("HNSW indeks tayyor: law_chunks_embedding_hnsw_idx");
        } catch (Exception e) {
            log.warn("HNSW indeks yaratib bo'lmadi: {}", e.getMessage());
        }
    }
}
