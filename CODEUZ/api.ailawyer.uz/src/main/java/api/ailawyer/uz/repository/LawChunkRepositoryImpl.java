package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.util.PgVectorUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

public class LawChunkRepositoryImpl implements LawChunkRepositoryCustom {

    private static final String FIND_SIMILAR_CHUNKS_SQL =
            "SELECT c.* FROM law_chunks c JOIN legal_documents d ON c.document_id = d.id "
                    + "WHERE d.status IN ('ACTIVE', 'PARTIALLY_AMENDED') AND c.embedding IS NOT NULL "
                    + "ORDER BY c.embedding <=> CAST(:queryVector AS vector) LIMIT :topK";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<LawChunkEntity> findSimilarChunks(float[] queryVector, int topK) {
        return entityManager.createNativeQuery(FIND_SIMILAR_CHUNKS_SQL, LawChunkEntity.class)
                .setParameter("queryVector", PgVectorUtils.toVectorLiteral(queryVector))
                .setParameter("topK", topK)
                .getResultList();
    }
}
