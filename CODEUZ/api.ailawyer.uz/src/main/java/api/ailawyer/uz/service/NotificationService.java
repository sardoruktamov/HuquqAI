package api.ailawyer.uz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kelajakda Push Notification (FCM) ulash uchun hook.
 * Hozircha faqat log.info qiladi.
 */
@Service
@Slf4j
public class NotificationService {

    public void notifyNewMessage(String title, String body, Map<String, Object> payload) {
        log.info("NOTIFICATION -> title={}, body={}, payload={}", title, body, payload);
    }
}

