package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.AiChatEntity;
import api.ailawyer.uz.enums.AiChatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiChatRepository extends JpaRepository<AiChatEntity, UUID> {
    Page<AiChatEntity> findAllByClientIdOrderByCreatedDateDesc(Integer clientId, Pageable pageable);
    Page<AiChatEntity> findAllByClientIdAndStatusOrderByCreatedDateDesc(Integer clientId, AiChatStatus status, Pageable pageable);
}

