package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerMessageAttachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerMessageAttachRepository extends JpaRepository<LawyerMessageAttachEntity, UUID> {
    List<LawyerMessageAttachEntity> findAllByMessageId(UUID messageId);
    List<LawyerMessageAttachEntity> findAllByMessageIdIn(List<UUID> messageIds);
}

