package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.lawyerchat.LawyerChatDTO;
import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.entity.LawyerMessageEntity;
import api.ailawyer.uz.entity.ProfileEntity;
import api.ailawyer.uz.enums.LawyerChatStatus;
import api.ailawyer.uz.enums.LawyerMessageSenderType;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerChatRepository;
import api.ailawyer.uz.repository.LawyerMessageRepository;
import api.ailawyer.uz.repository.ProfileRepository;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Advokat chatlari biznes logikasi.
 * <p>
 * Vazifalari:
 * <ul>
 *   <li>Joriy foydalanuvchining chatlar ro'yxatini qaytarish (mijoz, advokat yoki admin)</li>
 *   <li>Chat detail ko'rsatish (ism, rasm, oxirgi xabar bilan)</li>
 *   <li>Yangi chat yaratish yoki mavjud ACTIVE chatni qaytarish</li>
 *   <li>Chatni yopish (CLOSED)</li>
 *   <li>Kirish huquqini tekshirish — faqat ishtirokchilar ko'ra oladi</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LawyerChatService {

    /** lawyer_chat jadvali bilan ishlash */
    private final LawyerChatRepository lawyerChatRepository;
    /** Oxirgi xabar preview uchun lawyer_message jadvali */
    private final LawyerMessageRepository lawyerMessageRepository;
    /** Mijoz/advokat ism va rasm ma'lumotlari */
    private final ProfileRepository profileRepository;
    /** Profil rasmlarini DTO ga aylantirish */
    private final AttachService attachService;

    /**
     * Joriy kirgan foydalanuvchining chatlar ro'yxatini qaytaradi.
     * <p>
     * Rol bo'yicha filter:
     * - Admin/Superadmin — barcha chatlar
     * - Advokat — o'ziga kelgan chatlar
     * - Mijoz — o'zi boshlagan chatlar
     *
     * @param page sahifa raqami (0-based)
     * @param size sahifa hajmi
     */
    public PageImpl<LawyerChatDTO> getMyChats(int page, int size) {
        Integer me = SpringSecurityUtil.getCurrentUserId();
        PageRequest pr = PageRequest.of(page, size);

        Page<LawyerChatEntity> p;
        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN) || SpringSecurityUtil.hazRole(ProfileRole.ROLE_SUPERADMIN)) {
            p = lawyerChatRepository.findAll(pr);
        } else if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_LAWYER)) {
            p = lawyerChatRepository.findAllByLawyerIdOrderByCreatedDateDesc(me, pr);
        } else {
            p = lawyerChatRepository.findAllByClientIdOrderByCreatedDateDesc(me, pr);
        }

        List<LawyerChatEntity> chats = p.getContent();
        Map<Integer, ProfileEntity> profiles = loadProfiles(chats);
        Map<UUID, LawyerMessageEntity> lastMessages = loadLastMessages(chats);

        List<LawyerChatDTO> list = chats.stream()
                .map(chat -> toDto(chat, profiles, lastMessages, me))
                .toList();
        return new PageImpl<>(list, pr, p.getTotalElements());
    }

    /**
     * Bitta chatning to'liq ma'lumotini qaytaradi.
     * Ishtirokchi bo'lmasa xato tashlaydi.
     *
     * @param id chat UUID si
     */
    public LawyerChatDTO getById(UUID id) {
        LawyerChatEntity e = getEntityForRead(id);
        Map<Integer, ProfileEntity> profiles = loadProfiles(List.of(e));
        Map<UUID, LawyerMessageEntity> lastMessages = loadLastMessages(List.of(e));
        return toDto(e, profiles, lastMessages, SpringSecurityUtil.getCurrentUserId());
    }

    /**
     * Chatni o'qish uchun oladi va kirish huquqini tekshiradi.
     * LawyerMessageService list() va boshqa o'qish operatsiyalari uchun ishlatiladi.
     *
     * @param id chat UUID si
     */
    public LawyerChatEntity getEntityForRead(UUID id) {
        LawyerChatEntity e = lawyerChatRepository.findById(id).orElseThrow(() -> new AppBadException("Lawyer chat topilmadi!"));
        requireCanRead(e);
        return e;
    }

    /**
     * Chatga yozish uchun oladi — yopilgan (CLOSED) chatda xato tashlaydi.
     * LawyerMessageService sendMessage() chaqiradi.
     *
     * @param id chat UUID si
     */
    public LawyerChatEntity getEntityForWrite(UUID id) {
        LawyerChatEntity e = getEntityForRead(id);
        if (e.getStatus() == LawyerChatStatus.CLOSED) {
            throw new AppBadException("Lawyer chat yopilgan!");
        }
        return e;
    }

    /**
     * Mijoz va advokat o'rtasida faol chat mavjudligini ta'minlaydi.
     * Mavjud ACTIVE chat bo'lsa qaytaradi, yo'q bo'lsa yangi yaratadi.
     *
     * @param clientId mijoz profile id si
     * @param lawyerId advokat profile id si
     */
    public LawyerChatEntity ensureActiveChat(Integer clientId, Integer lawyerId) {
        return lawyerChatRepository
                .findByClientIdAndLawyerIdAndStatus(clientId, lawyerId, LawyerChatStatus.ACTIVE)
                .orElseGet(() -> {
                    LawyerChatEntity e = new LawyerChatEntity();
                    e.setClientId(clientId);
                    e.setLawyerId(lawyerId);
                    e.setStatus(LawyerChatStatus.ACTIVE);
                    e.setCreatedDate(LocalDateTime.now());
                    return lawyerChatRepository.save(e);
                });
    }

    /**
     * Chatni yopadi — status CLOSED ga o'zgaradi.
     * Yopilgan chatga yangi xabar yozib bo'lmaydi.
     *
     * @param id chat UUID si
     * @return muvaffaqiyat xabari
     */
    public String close(UUID id) {
        LawyerChatEntity chat = getEntityForRead(id);
        if (chat.getStatus() == LawyerChatStatus.CLOSED) {
            throw new AppBadException("Lawyer chat allaqachon yopilgan!");
        }
        chat.setStatus(LawyerChatStatus.CLOSED);
        lawyerChatRepository.save(chat);
        return "Lawyer chat yopildi";
    }

    /**
     * Yuboruvchi o'z xabarini o'qilgan deb belgilaydi.
     */
    public void markAsReadForSender(LawyerChatEntity chat, LawyerMessageSenderType senderType, UUID messageId) {
        if (senderType == LawyerMessageSenderType.USER) {
            chat.setLastReadMessageIdByClient(messageId);
        } else {
            chat.setLastReadMessageIdByLawyer(messageId);
        }
        lawyerChatRepository.save(chat);
    }

    /**
     * Chat xabarlarini ochganda eng oxirgi xabargacha o'qilgan deb belgilaydi.
     */
    public void markAsReadToLatest(LawyerChatEntity chat) {
        Integer me = SpringSecurityUtil.getCurrentUserId();
        lawyerMessageRepository.findFirstByLawyerChatIdOrderByCreatedDateDesc(chat.getId())
                .ifPresent(latest -> {
                    if (chat.getClientId().equals(me)) {
                        chat.setLastReadMessageIdByClient(latest.getId());
                    } else if (chat.getLawyerId().equals(me)) {
                        chat.setLastReadMessageIdByLawyer(latest.getId());
                    } else {
                        return;
                    }
                    lawyerChatRepository.save(chat);
                });
    }

    /** Foydalanuvchi chatni ko'rish huquqiga ega ekanini tekshiradi */
    private void requireCanRead(LawyerChatEntity e) {
        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN) || SpringSecurityUtil.hazRole(ProfileRole.ROLE_SUPERADMIN)) {
            return;
        }

        Integer me = SpringSecurityUtil.getCurrentUserId();

        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_LAWYER)) {
            if (e.getLawyerId().equals(me)) return;
        }

        if (e.getClientId().equals(me)) return;

        throw new AppBadException("Sizga bu lawyer chat'ga kirishga ruxsat yo'q!");
    }

    /** Chatlar ro'yxatidagi barcha mijoz va advokat profillarini yuklaydi */
    private Map<Integer, ProfileEntity> loadProfiles(List<LawyerChatEntity> chats) {
        if (chats.isEmpty()) {
            return Map.of();
        }
        Set<Integer> ids = new HashSet<>();
        for (LawyerChatEntity chat : chats) {
            ids.add(chat.getClientId());
            ids.add(chat.getLawyerId());
        }
        Map<Integer, ProfileEntity> map = new HashMap<>();
        for (Integer id : ids) {
            profileRepository.findByIdAndVisibleTrue(id).ifPresent(p -> map.put(id, p));
        }
        return map;
    }

    /** Har bir chat uchun eng oxirgi xabarni yuklaydi (chat ro'yxati preview) */
    private Map<UUID, LawyerMessageEntity> loadLastMessages(List<LawyerChatEntity> chats) {
        if (chats.isEmpty()) {
            return Map.of();
        }
        List<UUID> chatIds = chats.stream().map(LawyerChatEntity::getId).toList();
        List<LawyerMessageEntity> messages = lawyerMessageRepository.findAllByLawyerChatIdInOrderByCreatedDateDesc(chatIds);
        Map<UUID, LawyerMessageEntity> map = new HashMap<>();
        for (LawyerMessageEntity message : messages) {
            map.putIfAbsent(message.getLawyerChatId(), message);
        }
        return map;
    }

    /** Entity ni mobil uchun boyitilgan LawyerChatDTO ga aylantiradi */
    private LawyerChatDTO toDto(LawyerChatEntity e,
                                Map<Integer, ProfileEntity> profiles,
                                Map<UUID, LawyerMessageEntity> lastMessages,
                                Integer viewerProfileId) {
        LawyerChatDTO dto = new LawyerChatDTO();
        dto.setId(e.getId());
        dto.setClientId(e.getClientId());
        dto.setLawyerId(e.getLawyerId());
        dto.setStatus(e.getStatus());
        dto.setCreatedDate(e.getCreatedDate());

        ProfileEntity client = profiles.get(e.getClientId());
        if (client != null) {
            dto.setClientName(client.getFullName());
            dto.setClientPhoto(attachService.attachDTO(client.getPhotoId()));
        }

        ProfileEntity lawyer = profiles.get(e.getLawyerId());
        if (lawyer != null) {
            dto.setLawyerName(lawyer.getFullName());
            dto.setLawyerPhoto(attachService.attachDTO(lawyer.getPhotoId()));
        }

        LawyerMessageEntity lastMessage = lastMessages.get(e.getId());
        if (lastMessage != null) {
            dto.setLastMessageContent(lastMessage.getContent());
            dto.setLastMessageDate(lastMessage.getCreatedDate());
        }

        dto.setUnreadCount(resolveUnreadCount(e, viewerProfileId));
        return dto;
    }

    private long resolveUnreadCount(LawyerChatEntity chat, Integer viewerProfileId) {
        if (viewerProfileId == null) {
            return 0L;
        }
        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN)
                || SpringSecurityUtil.hazRole(ProfileRole.ROLE_SUPERADMIN)) {
            return 0L;
        }

        UUID lastReadMessageId;
        LawyerMessageSenderType oppositeSender;

        if (chat.getClientId().equals(viewerProfileId)) {
            lastReadMessageId = chat.getLastReadMessageIdByClient();
            oppositeSender = LawyerMessageSenderType.LAWYER;
        } else if (chat.getLawyerId().equals(viewerProfileId)) {
            lastReadMessageId = chat.getLastReadMessageIdByLawyer();
            oppositeSender = LawyerMessageSenderType.USER;
        } else {
            return 0L;
        }

        LocalDateTime afterCreatedDate = null;
        if (lastReadMessageId != null) {
            afterCreatedDate = lawyerMessageRepository.findById(lastReadMessageId)
                    .map(LawyerMessageEntity::getCreatedDate)
                    .orElse(null);
        }

        return lawyerMessageRepository.countUnreadMessages(chat.getId(), oppositeSender, afterCreatedDate);
    }
}
