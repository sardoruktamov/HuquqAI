package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.aichat.AiChatCreateDTO;
import api.ailawyer.uz.dto.aichat.AiChatDTO;
import api.ailawyer.uz.entity.AiChatEntity;
import api.ailawyer.uz.enums.AiChatStatus;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.AiChatRepository;
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
 * AI chatlar CRUD va access nazorati.
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiChatRepository aiChatRepository;

    public AiChatDTO create(AiChatCreateDTO dto) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();

        AiChatEntity e = new AiChatEntity();
        e.setClientId(clientId);
        e.setTitle(dto.getTitle());
        e.setStatus(AiChatStatus.ACTIVE);
        e.setCreatedDate(LocalDateTime.now());
        aiChatRepository.save(e);

        return toDto(e);
    }

    public PageImpl<AiChatDTO> getMyChats(int page, int size) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();
        PageRequest pr = PageRequest.of(page, size);

        Page<AiChatEntity> p = aiChatRepository.findAllByClientIdOrderByCreatedDateDesc(clientId, pr);
        List<AiChatDTO> list = p.getContent().stream().map(this::toDto).toList();

        return new PageImpl<>(list, pr, p.getTotalElements());
    }

    public AiChatDTO getById(UUID id) {
        AiChatEntity e = getEntityForRead(id);
        return toDto(e);
    }

    public String close(UUID id) {
        AiChatEntity e = getEntityForWrite(id);
        e.setStatus(AiChatStatus.CLOSED);
        aiChatRepository.save(e);
        return "AI chat yopildi";
    }

    public AiChatEntity getEntityForRead(UUID id) {
        AiChatEntity e = aiChatRepository.findById(id).orElseThrow(() -> new AppBadException("AI chat topilmadi!"));
        requireCanRead(e);
        return e;
    }

    public AiChatEntity getEntityForWrite(UUID id) {
        AiChatEntity e = getEntityForRead(id);
        if (e.getStatus() == AiChatStatus.CLOSED) {
            throw new AppBadException("AI chat yopilgan!");
        }
        return e;
    }

    private void requireCanRead(AiChatEntity e) {
        if (SpringSecurityUtil.hazRole(ProfileRole.ROLE_ADMIN) || SpringSecurityUtil.hazRole(ProfileRole.ROLE_SUPERADMIN)) {
            return;
        }
        Integer me = SpringSecurityUtil.getCurrentUserId();
        if (!e.getClientId().equals(me)) {
            throw new AppBadException("Sizga bu AI chat'ga kirishga ruxsat yo'q!");
        }
    }

    private AiChatDTO toDto(AiChatEntity e) {
        AiChatDTO dto = new AiChatDTO();
        dto.setId(e.getId());
        dto.setClientId(e.getClientId());
        dto.setTitle(e.getTitle());
        dto.setStatus(e.getStatus());
        dto.setCreatedDate(e.getCreatedDate());
        return dto;
    }
}

