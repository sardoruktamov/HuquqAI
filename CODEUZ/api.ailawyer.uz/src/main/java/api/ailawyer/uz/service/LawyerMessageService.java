package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.dto.lawyerchat.LawyerChatStartDTO;
import api.ailawyer.uz.dto.lawyerchat.LawyerMessageCreateDTO;
import api.ailawyer.uz.dto.lawyerchat.LawyerMessageDTO;
import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.entity.LawyerMessageAttachEntity;
import api.ailawyer.uz.entity.LawyerMessageEntity;
import api.ailawyer.uz.enums.LawyerMessageSenderType;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerMessageAttachRepository;
import api.ailawyer.uz.repository.LawyerMessageRepository;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Advokat chat xabarlari biznes logikasi.
 * <p>
 * Vazifalari:
 * <ul>
 *   <li>Mijoz advokatga birinchi xabar yozganda chat boshlash</li>
 *   <li>Mijoz yoki advokat chat ichiga xabar yuborish</li>
 *   <li>Xabarlar tarixini olish (fayl biriktirish bilan)</li>
 *   <li>Yangi xabar kelganda bildirishnoma yuborish (hozircha log)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LawyerMessageService {

    /** Chat yaratish, huquq tekshiruvi */
    private final LawyerChatService lawyerChatService;
    /** Xabarlarni saqlash va olish */
    private final LawyerMessageRepository lawyerMessageRepository;
    /** Xabarga biriktirilgan fayllar */
    private final LawyerMessageAttachRepository lawyerMessageAttachRepository;
    /** Fayl ma'lumotlarini DTO ga aylantirish */
    private final AttachService attachService;
    /** Yangi xabar haqida bildirishnoma (FCM hook) */
    private final NotificationService notificationService;
    /** Advokat tasdiqlanganligini tekshirish */
    private final LawyerProfileService lawyerProfileService;

    /**
     * Chat ichidagi xabarlar tarixini qaytaradi.
     * Faqat chat ishtirokchisi ko'ra oladi.
     *
     * @param lawyerChatId chat UUID si
     * @param page         sahifa (0-based)
     * @param size         sahifa hajmi
     */
    public PageImpl<LawyerMessageDTO> list(UUID lawyerChatId, int page, int size) {
        LawyerChatEntity chat = lawyerChatService.getEntityForRead(lawyerChatId);

        PageRequest pr = PageRequest.of(page, size);
        Page<LawyerMessageEntity> p = lawyerMessageRepository.findAllByLawyerChatIdOrderByCreatedDateDesc(chat.getId(), pr);

        List<LawyerMessageEntity> messages = p.getContent();
        Map<UUID, List<AttachDTO>> attachments = getAttachmentMap(messages);

        List<LawyerMessageDTO> list = messages.stream()
                .map(m -> toDto(m, attachments.getOrDefault(m.getId(), List.of())))
                .toList();

        lawyerChatService.markAsReadToLatest(chat);

        return new PageImpl<>(list, pr, p.getTotalElements());
    }

    /**
     * Mijoz advokatga birinchi xabar yozganda chat boshlanadi.
     * <p>
     * Ketma-ketlik:
     * 1. Advokat roli bo'lgan foydalanuvchi bu endpointdan foydalana olmaydi
     * 2. Tanlangan advokat tasdiqlangan (APPROVED) ekanligi tekshiriladi
     * 3. ACTIVE chat yaratiladi yoki mavjud chat olinadi
     * 4. Xabar saqlanadi va advokatga bildirishnoma yuboriladi
     *
     * @param dto advokat id si va birinchi xabar matni
     */
    public LawyerMessageDTO startChatAndSend(LawyerChatStartDTO dto) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();
        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_LAWYER)) {
            throw new AppBadException("Advokat bu endpoint orqali chat boshlay olmaydi!");
        }

        lawyerProfileService.requireApprovedLawyer(dto.getLawyerId());

        LawyerChatEntity chat = lawyerChatService.ensureActiveChat(clientId, dto.getLawyerId());
        LawyerMessageDTO saved = saveMessage(chat, LawyerMessageSenderType.USER, dto.getMessage());

        notificationService.notifyLawyerNewMessage(chat.getLawyerId(), chat.getId());

        return saved;
    }

    /**
     * Mavjud chatga yangi xabar yuboradi.
     * Mijoz USER, advokat LAWYER sifatida belgilanadi.
     * Yopilgan chatga yozish mumkin emas.
     *
     * @param lawyerChatId chat UUID si
     * @param dto          xabar matni va ixtiyoriy fayl id lar
     */
    public LawyerMessageDTO sendMessage(UUID lawyerChatId, LawyerMessageCreateDTO dto) {
        LawyerChatEntity chat = lawyerChatService.getEntityForWrite(lawyerChatId);

        Integer me = SpringSecurityUtil.getCurrentUserId();
        boolean isAdmin = SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN) || SpringSecurityUtil.hazRole(ProfileRole.ROLE_SUPERADMIN);

        LawyerMessageSenderType senderType;
        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_LAWYER)) {
            if (!isAdmin && !chat.getLawyerId().equals(me)) {
                throw new AppBadException("Sizga bu chatga advokat sifatida yozishga ruxsat yo'q!");
            }
            senderType = LawyerMessageSenderType.LAWYER;
        } else {
            if (!isAdmin && !chat.getClientId().equals(me)) {
                throw new AppBadException("Sizga bu chatga yozishga ruxsat yo'q!");
            }
            senderType = LawyerMessageSenderType.USER;
        }

        LawyerMessageDTO saved = saveMessage(chat, senderType, dto);

        if (senderType == LawyerMessageSenderType.USER) {
            notificationService.notifyLawyerNewMessage(chat.getLawyerId(), chat.getId());
        } else {
            notificationService.notifyClientNewMessage(chat.getClientId(), chat.getId());
        }

        return saved;
    }

    /** Xabarni bazaga saqlaydi va biriktirilgan fayllarni bog'laydi */
    private LawyerMessageDTO saveMessage(LawyerChatEntity chat, LawyerMessageSenderType senderType, LawyerMessageCreateDTO dto) {
        LawyerMessageEntity m = new LawyerMessageEntity();
        m.setLawyerChatId(chat.getId());
        m.setSenderType(senderType);
        m.setContent(dto.getContent());
        m.setCreatedDate(LocalDateTime.now());
        lawyerMessageRepository.save(m);

        lawyerChatService.markAsReadForSender(chat, senderType, m.getId());

        List<AttachDTO> attachments = saveAttachments(m.getId(), dto.getAttachIds());
        return toDto(m, attachments);
    }

    /** Xabarga fayl(lar) biriktiradi va AttachDTO ro'yxatini qaytaradi */
    private List<AttachDTO> saveAttachments(UUID messageId, List<String> attachIds) {
        if (attachIds == null || attachIds.isEmpty()) return List.of();

        List<AttachDTO> list = new ArrayList<>();
        for (String attachId : attachIds) {
            if (attachId == null || attachId.isBlank()) continue;
            attachService.getEntity(attachId);

            LawyerMessageAttachEntity link = new LawyerMessageAttachEntity();
            link.setMessageId(messageId);
            link.setAttachId(attachId);
            link.setCreatedDate(LocalDateTime.now());
            lawyerMessageAttachRepository.save(link);

            list.add(attachService.attachDTO(attachId));
        }
        return list;
    }

    /** Bir nechta xabar uchun biriktirilgan fayllarni batch yuklaydi */
    private Map<UUID, List<AttachDTO>> getAttachmentMap(List<LawyerMessageEntity> messages) {
        if (messages.isEmpty()) return Map.of();

        List<UUID> ids = messages.stream().map(LawyerMessageEntity::getId).toList();
        List<LawyerMessageAttachEntity> links = lawyerMessageAttachRepository.findAllByMessageIdIn(ids);

        Map<UUID, List<AttachDTO>> map = new HashMap<>();
        for (LawyerMessageAttachEntity l : links) {
            map.computeIfAbsent(l.getMessageId(), k -> new ArrayList<>())
                    .add(attachService.attachDTO(l.getAttachId()));
        }
        return map;
    }

    /** Entity ni API javobi uchun LawyerMessageDTO ga aylantiradi */
    private LawyerMessageDTO toDto(LawyerMessageEntity e, List<AttachDTO> attachments) {
        LawyerMessageDTO dto = new LawyerMessageDTO();
        dto.setId(e.getId());
        dto.setLawyerChatId(e.getLawyerChatId());
        dto.setSenderType(e.getSenderType());
        dto.setContent(e.getContent());
        dto.setCreatedDate(e.getCreatedDate());
        dto.setAttachments(attachments);
        return dto;
    }
}
