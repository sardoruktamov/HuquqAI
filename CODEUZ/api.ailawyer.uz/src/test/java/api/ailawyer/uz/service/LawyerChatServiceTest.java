package api.ailawyer.uz.service;

import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.enums.LawyerChatStatus;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerChatRepository;
import api.ailawyer.uz.repository.LawyerMessageRepository;
import api.ailawyer.uz.repository.ProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LawyerChatServiceTest {

    @Mock
    private LawyerChatRepository lawyerChatRepository;
    @Mock
    private LawyerMessageRepository lawyerMessageRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private AttachService attachService;

    @InjectMocks
    private LawyerChatService lawyerChatService;

    private UUID chatId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        TestSecurityHelper.loginAs(1, ProfileRole.ROLE_USER);
    }

    @AfterEach
    void tearDown() {
        TestSecurityHelper.clear();
    }

    @Test
    void getEntityForWrite_throwsWhenChatClosed() {
        LawyerChatEntity chat = activeChat();
        chat.setStatus(LawyerChatStatus.CLOSED);
        when(lawyerChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerChatService.getEntityForWrite(chatId));

        assertEquals("Lawyer chat yopilgan!", ex.getMessage());
    }

    @Test
    void close_throwsWhenAlreadyClosed() {
        LawyerChatEntity chat = activeChat();
        chat.setStatus(LawyerChatStatus.CLOSED);
        when(lawyerChatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerChatService.close(chatId));

        assertEquals("Lawyer chat allaqachon yopilgan!", ex.getMessage());
    }

    @Test
    void close_closesActiveChat() {
        LawyerChatEntity chat = activeChat();
        when(lawyerChatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(lawyerChatRepository.save(chat)).thenReturn(chat);

        String result = lawyerChatService.close(chatId);

        assertEquals("Lawyer chat yopildi", result);
        assertEquals(LawyerChatStatus.CLOSED, chat.getStatus());
    }

    private LawyerChatEntity activeChat() {
        LawyerChatEntity chat = new LawyerChatEntity();
        chat.setId(chatId);
        chat.setClientId(1);
        chat.setLawyerId(2);
        chat.setStatus(LawyerChatStatus.ACTIVE);
        chat.setCreatedDate(LocalDateTime.now());
        return chat;
    }
}
