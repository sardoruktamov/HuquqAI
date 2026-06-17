package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.lawyerchat.LawyerChatStartDTO;
import api.ailawyer.uz.dto.lawyerchat.LawyerMessageCreateDTO;
import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.enums.LawyerChatStatus;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerMessageAttachRepository;
import api.ailawyer.uz.repository.LawyerMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LawyerMessageServiceTest {

    @Mock
    private LawyerChatService lawyerChatService;
    @Mock
    private LawyerMessageRepository lawyerMessageRepository;
    @Mock
    private LawyerMessageAttachRepository lawyerMessageAttachRepository;
    @Mock
    private AttachService attachService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LawyerProfileService lawyerProfileService;

    @InjectMocks
    private LawyerMessageService lawyerMessageService;

    @BeforeEach
    void setUp() {
        TestSecurityHelper.loginAs(10, ProfileRole.ROLE_USER);
    }

    @AfterEach
    void tearDown() {
        TestSecurityHelper.clear();
    }

    @Test
    void startChatAndSend_callsRequireApprovedLawyer() {
        LawyerChatStartDTO dto = new LawyerChatStartDTO();
        dto.setLawyerId(5);
        LawyerMessageCreateDTO message = new LawyerMessageCreateDTO();
        message.setContent("Salom");
        dto.setMessage(message);

        LawyerChatEntity chat = new LawyerChatEntity();
        chat.setId(UUID.randomUUID());
        chat.setClientId(10);
        chat.setLawyerId(5);
        chat.setStatus(LawyerChatStatus.ACTIVE);
        chat.setCreatedDate(LocalDateTime.now());

        when(lawyerChatService.ensureActiveChat(10, 5)).thenReturn(chat);
        when(lawyerMessageRepository.save(any())).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, api.ailawyer.uz.entity.LawyerMessageEntity.class);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        lawyerMessageService.startChatAndSend(dto);

        verify(lawyerProfileService).requireApprovedLawyer(5);
        verify(lawyerChatService).ensureActiveChat(10, 5);
        verify(notificationService).notifyLawyerNewMessage(5, chat.getId());
    }

    @Test
    void startChatAndSend_rejectsLawyerRole() {
        TestSecurityHelper.clear();
        TestSecurityHelper.loginAs(10, ProfileRole.ROLE_LAWYER);

        LawyerChatStartDTO dto = new LawyerChatStartDTO();
        dto.setLawyerId(5);
        LawyerMessageCreateDTO message = new LawyerMessageCreateDTO();
        message.setContent("Salom");
        dto.setMessage(message);

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerMessageService.startChatAndSend(dto));

        assertEquals("Advokat bu endpoint orqali chat boshlay olmaydi!", ex.getMessage());
        verifyNoInteractions(lawyerProfileService);
    }

    @Test
    void sendMessage_throwsWhenChatClosed() {
        UUID chatId = UUID.randomUUID();
        when(lawyerChatService.getEntityForWrite(chatId))
                .thenThrow(new AppBadException("Lawyer chat yopilgan!"));

        LawyerMessageCreateDTO dto = new LawyerMessageCreateDTO();
        dto.setContent("Test");

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerMessageService.sendMessage(chatId, dto));

        assertEquals("Lawyer chat yopilgan!", ex.getMessage());
    }
}
