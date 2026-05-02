package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.CaseEntity;
import api.ailawyer.uz.enums.CaseStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseRepository extends CrudRepository<CaseEntity, String>, PagingAndSortingRepository<CaseEntity, String> {

    Page<CaseEntity> findAllByClientIdOrderByCreatedDateDesc(Integer clientId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("update CaseEntity set status = ?2 where id = ?1 and clientId = ?3")
    int changeStatus(String id, CaseStatus status, Integer clientId);
}