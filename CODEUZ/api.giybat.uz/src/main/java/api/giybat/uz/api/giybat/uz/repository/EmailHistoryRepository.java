package api.giybat.uz.api.giybat.uz.repository;

import api.giybat.uz.api.giybat.uz.entity.EmailHistoryEntity;
import api.giybat.uz.api.giybat.uz.entity.SmsHistoryEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailHistoryRepository extends CrudRepository<EmailHistoryEntity,String> {

    //select count(*) from email_history where email =? and created_date between ? and ?
    Long countByEmailAndCreatedDateBetween(String phoneNumber, LocalDateTime from, LocalDateTime to);

    //select * from email_history where email =? order by created_date desc limit 1
    Optional<EmailHistoryEntity> findTop1ByEmailOrderByCreatedDateDesc(String email);

    // 3 martadan ortiq smsni tasdiqlash kodini yuborganda limit qoyildi
    @Modifying
    @Transactional
    @Query("update EmailHistoryEntity set attemptCount = coalesce(attemptCount, 0) + 1 where id =?1")
    void updateAttemptCount(Integer id);
}
