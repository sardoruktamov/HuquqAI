package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.AiMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessageEntity, UUID> {
    Page<AiMessageEntity> findAllByAiChatIdOrderByCreatedDateDesc(UUID aiChatId, Pageable pageable);
}

