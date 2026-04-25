package api.giybat.uz.api.giybat.uz.service;

import api.giybat.uz.api.giybat.uz.entity.SmsHistoryEntity;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.enums.SmsType;
import api.giybat.uz.api.giybat.uz.exps.AppBadException;
import api.giybat.uz.api.giybat.uz.repository.SmsHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class SmsHistoryService {

    @Autowired
    private SmsHistoryRepository smsHistoryRepository;
    @Autowired
    private ResourceBundleService bundleService;
    public void created(String phone, String message, String code, SmsType smsType){
        SmsHistoryEntity entity = new SmsHistoryEntity();
        entity.setPhoneNumber(phone);
        entity.setMessage(message);
        entity.setCode(code);
        entity.setSmsType(smsType);
        entity.setAttemptCount(0);
        entity.setCreatedDate(LocalDateTime.now());
        smsHistoryRepository.save(entity);
    }

    public Long getSmsCount(String phone){      // 1 minutni ichida sms yuborishni tekshirish
        LocalDateTime now = LocalDateTime.now();
        return smsHistoryRepository.countByPhoneNumberAndCreatedDateBetween(phone,now.minusMinutes(1),now);
    }

    public void check(String phoneNumber, String code, AppLanguage lang){
        Optional<SmsHistoryEntity> optional = smsHistoryRepository.findTop1ByPhoneNumberOrderByCreatedDateDesc(phoneNumber);
        if (optional.isEmpty()){
            log.warn("Verification failed in checked: {}",phoneNumber);
            throw new AppBadException(bundleService.getMessage("verification.failed",lang));
        }
        // chacking code
        SmsHistoryEntity entity = optional.get();

        if (entity.getAttemptCount() >= 3){
            log.warn("Number attempts expired: {}",phoneNumber);
            throw new AppBadException(bundleService.getMessage("number.attempts.expired",lang));
        }
        if (!entity.getCode().equals(code)){
            smsHistoryRepository.updateAttemptCount(entity.getId());
            log.warn("Code verification failed in checked: {}",phoneNumber);
            throw new AppBadException(bundleService.getMessage("verification.failed",lang));
        }
        //check time
        LocalDateTime expDate = entity.getCreatedDate().plusMinutes(2);
        if (LocalDateTime.now().isAfter(expDate)) {                               //agar hozirgi vaqt expDatedan keyin(katta yani o'tib ketgan) bo'lsa,
            log.warn("Time sms code expired: {}",phoneNumber);      // yani 2minut vaqt o'tib ketgan bo'lsa
            throw new AppBadException(bundleService.getMessage("time.sms.code.expired",lang));

        }
    }

}
