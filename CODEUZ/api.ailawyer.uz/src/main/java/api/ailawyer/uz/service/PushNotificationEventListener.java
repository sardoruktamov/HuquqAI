package api.ailawyer.uz.service;

import api.ailawyer.uz.event.PushNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Push eventlarni fonda qabul qilib FCM orqali yuboradi.
 * Asosiy request thread bloklanmaydi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationEventListener {

    private final FcmNotificationSender fcmNotificationSender;

    @Async("notificationExecutor")
    @EventListener
    public void handlePushNotification(PushNotificationEvent event) {
        log.info("Push event qabul qilindi type={}, targets={}", event.type(), event.targetProfileIds());

        if (event.targetProfileIds() == null || event.targetProfileIds().isEmpty()) {
            return;
        }

        for (Integer profileId : event.targetProfileIds()) {
            if (profileId == null) {
                continue;
            }
            fcmNotificationSender.sendToProfile(profileId, event.title(), event.body(), event.data());
        }
    }
}
