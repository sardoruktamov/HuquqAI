package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawChunkRepository extends JpaRepository<LawChunkEntity, UUID> {

    List<LawChunkEntity> findAllByDocumentIdOrderByArticleRefAsc(UUID documentId);

    Optional<LawChunkEntity> findByDocumentIdAndArticleRef(UUID documentId, String articleRef);

    void deleteAllByDocumentId(UUID documentId);
}
