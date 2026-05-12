package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.enums.LawyerChatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawyerChatRepository extends JpaRepository<LawyerChatEntity, UUID> {
    Page<LawyerChatEntity> findAllByClientIdOrderByCreatedDateDesc(Integer clientId, Pageable pageable);
    Page<LawyerChatEntity> findAllByLawyerIdOrderByCreatedDateDesc(Integer lawyerId, Pageable pageable);

    Optional<LawyerChatEntity> findByClientIdAndLawyerIdAndStatus(Integer clientId, Integer lawyerId, LawyerChatStatus status);
}

