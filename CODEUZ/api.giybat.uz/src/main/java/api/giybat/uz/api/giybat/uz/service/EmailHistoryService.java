package api.giybat.uz.api.giybat.uz.service;

import api.giybat.uz.api.giybat.uz.entity.EmailHistoryEntity;
import api.giybat.uz.api.giybat.uz.entity.SmsHistoryEntity;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.enums.SmsType;
import api.giybat.uz.api.giybat.uz.exps.AppBadException;
import api.giybat.uz.api.giybat.uz.repository.EmailHistoryRepository;
import api.giybat.uz.api.giybat.uz.repository.SmsHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailHistoryService {

    @Autowired
    private EmailHistoryRepository emailHistoryRepository;
    @Autowired
    private ResourceBundleService bundleService;

    public void created(String email, String code, SmsType emailType){
        EmailHistoryEntity entity = new EmailHistoryEntity();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setEmailType(emailType);
        entity.setAttemptCount(0);
        entity.setCreatedDate(LocalDateTime.now());
        emailHistoryRepository.save(entity);
    }

    public Long getEmailCount(String email){      // 1 minutni ichida sms yuborishni tekshirish
        LocalDateTime now = LocalDateTime.now();
        return emailHistoryRepository.countByEmailAndCreatedDateBetween(email,now.minusMinutes(1),now);
    }

    public void check(String email, String code, AppLanguage lang){
        Optional<EmailHistoryEntity> optional = emailHistoryRepository.findTop1ByEmailOrderByCreatedDateDesc(email);
        if (optional.isEmpty()){
            throw new AppBadException(bundleService.getMessage("verification.failed",lang));
        }
        // chacking code
        EmailHistoryEntity entity = optional.get();

        if (entity.getAttemptCount() >= 3){
            throw new AppBadException(bundleService.getMessage("number.attempts.expired",lang));
        }
        if (!entity.getCode().equals(code)){
            emailHistoryRepository.updateAttemptCount(entity.getId());
            throw new AppBadException(bundleService.getMessage("verification.failed",lang));
        }
        //check time
        LocalDateTime expDate = entity.getCreatedDate().plusMinutes(2);
        if (LocalDateTime.now().isAfter(expDate)) {                               //agar hozirgi vaqt expDatedan keyin(katta yani o'tib ketgan) bo'lsa,
            throw new AppBadException(bundleService.getMessage("time.sms.code.expired",lang));   // yani 2minut vaqt o'tib ketgan bo'lsa

        }
    }

}
