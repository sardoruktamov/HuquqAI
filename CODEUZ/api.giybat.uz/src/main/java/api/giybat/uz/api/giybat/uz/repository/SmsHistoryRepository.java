package api.giybat.uz.api.giybat.uz.repository;

import api.giybat.uz.api.giybat.uz.entity.SmsHistoryEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SmsHistoryRepository extends CrudRepository<SmsHistoryEntity,String> {

    //select count(*) from sms_history where phone_number =? and created_date between ? and ?
    Long countByPhoneNumberAndCreatedDateBetween(String phoneNumber, LocalDateTime from, LocalDateTime to);

    //select * from sms_history where phone_number =? order by created_date desc limit 1
    Optional<SmsHistoryEntity> findTop1ByPhoneNumberOrderByCreatedDateDesc(String phoneNumber);

    // 3 martadan ortiq smsni tasdiqlash kodini yuborganda limit qoyildi
    @Modifying
    @Transactional
    @Query("update SmsHistoryEntity set attemptCount = coalesce(attemptCount, 0) + 1 where id =?1")
    void updateAttemptCount(Integer id);
}
