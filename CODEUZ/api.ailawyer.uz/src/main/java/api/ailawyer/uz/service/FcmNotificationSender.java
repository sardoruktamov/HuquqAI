package api.ailawyer.uz.service;

import api.ailawyer.uz.config.FirebaseProperties;
import api.ailawyer.uz.entity.DeviceTokenEntity;
import api.ailawyer.uz.repository.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging orqali push yuborish.
 * O'lik yoki eskirgan tokenlarni avtomatik deaktivatsiya qiladi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmNotificationSender {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseProperties firebaseProperties;

    public void sendToProfile(Integer profileId, String title, String body, Map<String, String> data) {
        if (!firebaseProperties.isEnabled()) {
            log.info("FCM disabled — skip push profileId={}, title={}", profileId, title);
            return;
        }

        List<DeviceTokenEntity> tokens = deviceTokenRepository.findAllByProfileIdAndActiveTrue(profileId);
        if (tokens.isEmpty()) {
            log.debug("Aktiv device token topilmadi profileId={}", profileId);
            return;
        }

        for (DeviceTokenEntity deviceToken : tokens) {
            sendToToken(deviceToken, title, body, data);
        }
    }

    private void sendToToken(DeviceTokenEntity deviceToken, String title, String body, Map<String, String> data) {
        try {
            Map<String, String> payload = data != null ? new HashMap<>(data) : new HashMap<>();
            Message message = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(payload)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            deviceToken.setLastUsedDate(LocalDateTime.now());
            deviceTokenRepository.save(deviceToken);
            log.info("FCM yuborildi profileId={}, response={}", deviceToken.getProfileId(), response);
        } catch (FirebaseMessagingException e) {
            if (isDeadToken(e)) {
                log.warn("O'lik FCM token deaktivatsiya qilinmoqda profileId={}, code={}",
                        deviceToken.getProfileId(), e.getMessagingErrorCode());
                deviceToken.setActive(false);
                deviceTokenRepository.save(deviceToken);
            } else {
                log.error("FCM yuborish xatosi profileId={}: {}", deviceToken.getProfileId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("FCM umumiy xato profileId={}: {}", deviceToken.getProfileId(), e.getMessage());
        }
    }

    private boolean isDeadToken(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT) {
            return true;
        }
        String message = e.getMessage();
        return message != null && message.toUpperCase().contains("NOT_FOUND");
    }
}
