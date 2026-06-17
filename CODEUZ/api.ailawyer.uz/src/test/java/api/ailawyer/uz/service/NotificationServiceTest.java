package api.ailawyer.uz.service;

import api.ailawyer.uz.enums.NotificationType;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.event.PushNotificationEvent;
import api.ailawyer.uz.repository.ProfileRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ProfileRoleRepository profileRoleRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyLawyerNewMessage_publishesEvent() {
        UUID chatId = UUID.randomUUID();
        notificationService.notifyLawyerNewMessage(5, chatId);

        ArgumentCaptor<PushNotificationEvent> captor = ArgumentCaptor.forClass(PushNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        PushNotificationEvent event = captor.getValue();
        assertEquals(NotificationType.LAWYER_NEW_MESSAGE, event.type());
        assertEquals(List.of(5), event.targetProfileIds());
        assertEquals(chatId.toString(), event.data().get("chatId"));
    }

    @Test
    void notifyLawyerOnboardingPending_publishesToAdmins() {
        when(profileRoleRepository.findDistinctProfileIdsByRolesIn(
                List.of(ProfileRole.ROLE_ADMIN, ProfileRole.ROLE_SUPERADMIN)))
                .thenReturn(List.of(1, 2));

        notificationService.notifyLawyerOnboardingPending(7);

        ArgumentCaptor<PushNotificationEvent> captor = ArgumentCaptor.forClass(PushNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        PushNotificationEvent event = captor.getValue();
        assertEquals(NotificationType.LAWYER_ONBOARDING_PENDING, event.type());
        assertEquals(List.of(1, 2), event.targetProfileIds());
    }
}
