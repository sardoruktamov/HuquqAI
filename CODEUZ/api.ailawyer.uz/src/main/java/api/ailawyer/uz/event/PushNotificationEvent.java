package api.ailawyer.uz.event;

import api.ailawyer.uz.enums.NotificationType;

import java.util.List;
import java.util.Map;

/**
 * Push bildirishnoma yuborish uchun asinxron event.
 * ApplicationEventPublisher orqali publish qilinadi.
 */
public record PushNotificationEvent(
        NotificationType type,
        String title,
        String body,
        List<Integer> targetProfileIds,
        Map<String, String> data
) {
}
