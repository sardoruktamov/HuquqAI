package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.AiMessageAttachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiMessageAttachRepository extends JpaRepository<AiMessageAttachEntity, UUID> {
    List<AiMessageAttachEntity> findAllByMessageId(UUID messageId);
    List<AiMessageAttachEntity> findAllByMessageIdIn(List<UUID> messageIds);
}

