package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.lawyerchat.LawyerChatDTO;
import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.enums.LawyerChatStatus;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerChatRepository;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lawyer chatlar: list/detail va "ensure chat exists" (client birinchi xabar yozganda).
 */
@Service
@RequiredArgsConstructor
public class LawyerChatService {

    private final LawyerChatRepository lawyerChatRepository;

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

        List<LawyerChatDTO> list = p.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(list, pr, p.getTotalElements());
    }

    public LawyerChatDTO getById(UUID id) {
        LawyerChatEntity e = getEntityForRead(id);
        return toDto(e);
    }

    public LawyerChatEntity getEntityForRead(UUID id) {
        LawyerChatEntity e = lawyerChatRepository.findById(id).orElseThrow(() -> new AppBadException("Lawyer chat topilmadi!"));
        requireCanRead(e);
        return e;
    }

    public LawyerChatEntity getEntityForWrite(UUID id) {
        LawyerChatEntity e = getEntityForRead(id);
        if (e.getStatus() == LawyerChatStatus.CLOSED) {
            throw new AppBadException("Lawyer chat yopilgan!");
        }
        return e;
    }

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
     * Advokat chatini yopadi — status CLOSED ga o'zgaradi.
     * Faqat chat ishtirokchisi (mijoz, advokat yoki admin) chaqira oladi.
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

    private LawyerChatDTO toDto(LawyerChatEntity e) {
        LawyerChatDTO dto = new LawyerChatDTO();
        dto.setId(e.getId());
        dto.setClientId(e.getClientId());
        dto.setLawyerId(e.getLawyerId());
        dto.setStatus(e.getStatus());
        dto.setCreatedDate(e.getCreatedDate());
        return dto;
    }
}

