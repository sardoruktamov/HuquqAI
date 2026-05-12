package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LawyerMessageRepository extends JpaRepository<LawyerMessageEntity, UUID> {
    Page<LawyerMessageEntity> findAllByLawyerChatIdOrderByCreatedDateDesc(UUID lawyerChatId, Pageable pageable);
}

