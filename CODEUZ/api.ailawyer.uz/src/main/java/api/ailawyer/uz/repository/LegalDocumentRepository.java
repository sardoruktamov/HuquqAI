package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LegalDocumentEntity;
import api.ailawyer.uz.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocumentEntity, UUID> {

    Optional<LegalDocumentEntity> findByDocNumber(String docNumber);

    List<LegalDocumentEntity> findAllByStatus(DocumentStatus status);
}
