package api.ailawyer.uz.service;

import api.ailawyer.uz.enums.NotificationType;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.event.PushNotificationEvent;
import api.ailawyer.uz.repository.ProfileRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Push bildirishnomalar uchun event publisher.
 * FCM yuborish asinxron listener orqali bajariladi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final ProfileRoleRepository profileRoleRepository;

    public void notifyLawyerNewMessage(Integer lawyerId, UUID chatId) {
        Map<String, String> data = baseData(NotificationType.LAWYER_NEW_MESSAGE);
        data.put("chatId", chatId.toString());
        data.put("lawyerId", lawyerId.toString());
        publish(
                List.of(lawyerId),
                NotificationType.LAWYER_NEW_MESSAGE,
                "Sizda yangi xabar",
                "Mijozdan yangi xabar keldi",
                data
        );
    }

    public void notifyClientNewMessage(Integer clientId, UUID chatId) {
        Map<String, String> data = baseData(NotificationType.CLIENT_NEW_MESSAGE);
        data.put("chatId", chatId.toString());
        data.put("clientId", clientId.toString());
        publish(
                List.of(clientId),
                NotificationType.CLIENT_NEW_MESSAGE,
                "Sizda yangi xabar",
                "Advokatdan javob keldi",
                data
        );
    }

    public void notifyLawyerOnboardingPending(Integer lawyerProfileId) {
        List<Integer> adminIds = findAdminProfileIds();
        if (adminIds.isEmpty()) {
            log.warn("Admin profil topilmadi — onboarding pending push yuborilmadi");
            return;
        }

        Map<String, String> data = baseData(NotificationType.LAWYER_ONBOARDING_PENDING);
        data.put("profileId", lawyerProfileId.toString());
        publish(
                adminIds,
                NotificationType.LAWYER_ONBOARDING_PENDING,
                "Yangi advokat arizasi",
                "Tasdiqlash uchun yangi advokat profili yuborildi",
                data
        );
    }

    public void notifyLawyerOnboardingApproved(Integer lawyerProfileId) {
        Map<String, String> data = baseData(NotificationType.LAWYER_ONBOARDING_APPROVED);
        data.put("profileId", lawyerProfileId.toString());
        publish(
                List.of(lawyerProfileId),
                NotificationType.LAWYER_ONBOARDING_APPROVED,
                "Profil tasdiqlandi",
                "Advokat profilingiz admin tomonidan tasdiqlandi",
                data
        );
    }

    public void notifyLawyerOnboardingRejected(Integer lawyerProfileId, String reason) {
        Map<String, String> data = baseData(NotificationType.LAWYER_ONBOARDING_REJECTED);
        data.put("profileId", lawyerProfileId.toString());
        data.put("reason", reason != null ? reason : "");
        publish(
                List.of(lawyerProfileId),
                NotificationType.LAWYER_ONBOARDING_REJECTED,
                "Profil rad etildi",
                "Advokat profilingiz rad etildi. Sababni ko'rib chiqing",
                data
        );
    }

    private void publish(List<Integer> targetProfileIds,
                         NotificationType type,
                         String title,
                         String body,
                         Map<String, String> data) {
        eventPublisher.publishEvent(new PushNotificationEvent(type, title, body, targetProfileIds, data));
        log.debug("Push event publish qilindi type={}, targets={}", type, targetProfileIds);
    }

    private Map<String, String> baseData(NotificationType type) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        return data;
    }

    private List<Integer> findAdminProfileIds() {
        return profileRoleRepository.findDistinctProfileIdsByRolesIn(
                List.of(ProfileRole.ROLE_ADMIN, ProfileRole.ROLE_SUPERADMIN)
        );
    }
}
