package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.cases.CaseCreateDTO;
import api.ailawyer.uz.dto.cases.CaseDTO;
import api.ailawyer.uz.entity.CaseEntity;
import api.ailawyer.uz.enums.CaseStatus;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.CaseRepository;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CaseService {

    @Autowired
    private CaseRepository caseRepository;

    public CaseDTO create(CaseCreateDTO dto) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();

        CaseEntity entity = new CaseEntity();
        entity.setTitle(dto.getTitle());
        entity.setClientId(clientId);
        entity.setStatus(CaseStatus.OPEN);
        entity.setCreatedDate(LocalDateTime.now());

        caseRepository.save(entity);
        return toDto(entity);
    }

    public PageImpl<CaseDTO> getMyCases(int page, int size) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<CaseEntity> entityPage = caseRepository.findAllByClientIdOrderByCreatedDateDesc(clientId, pageRequest);

        List<CaseDTO> dtoList = entityPage.getContent().stream()
                .map(this::toDto)
                .toList();

        return new PageImpl<>(dtoList, pageRequest, entityPage.getTotalElements());
    }

    public CaseDTO getById(String id) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();

        CaseEntity entity = caseRepository.findById(id).orElseThrow(() -> {
            log.warn("Case not found: {}", id);
            return new AppBadException("Case topilmadi!");
        });

        // Birovning case'iga boshqasi kirmasligi uchun security check
        if (!entity.getClientId().equals(clientId)) {
            log.warn("User {} trying to access someone else's case {}", clientId, id);
            throw new AppBadException("Sizga bu sahifaga kirishga ruxsat yo'q!");
        }

        return toDto(entity);
    }

    public String closeCase(String id) {
        Integer clientId = SpringSecurityUtil.getCurrentUserId();
        int effectedRows = caseRepository.changeStatus(id, CaseStatus.CLOSED, clientId);

        if (effectedRows == 0) {
            throw new AppBadException("Case topilmadi yoki yopishga ruxsatingiz yo'q!");
        }

        return "Case muvaffaqiyatli yopildi!";
    }

    private CaseDTO toDto(CaseEntity entity) {
        CaseDTO dto = new CaseDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setStatus(entity.getStatus());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLawyerId(entity.getLawyerId());
        return dto;
    }
}