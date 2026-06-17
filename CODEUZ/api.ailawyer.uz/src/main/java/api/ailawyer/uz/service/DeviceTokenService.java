package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.notification.DeviceTokenRegisterDTO;
import api.ailawyer.uz.entity.DeviceTokenEntity;
import api.ailawyer.uz.repository.DeviceTokenRepository;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Login dan keyin FCM device tokenni ro'yxatdan o'tkazadi yoki yangilaydi.
     */
    @Transactional
    public String register(DeviceTokenRegisterDTO dto) {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        deviceTokenRepository.findByToken(dto.getToken()).ifPresent(existing -> {
            if (!existing.getProfileId().equals(profileId)) {
                existing.setActive(false);
                deviceTokenRepository.save(existing);
            }
        });

        DeviceTokenEntity entity = deviceTokenRepository
                .findByProfileIdAndToken(profileId, dto.getToken())
                .orElseGet(DeviceTokenEntity::new);

        entity.setProfileId(profileId);
        entity.setToken(dto.getToken().trim());
        entity.setPlatform(dto.getPlatform());
        entity.setActive(true);
        if (entity.getCreatedDate() == null) {
            entity.setCreatedDate(now);
        }
        entity.setLastUsedDate(now);
        deviceTokenRepository.save(entity);

        return "Device token ro'yxatdan o'tkazildi";
    }

    /**
     * Logout da tokenni deaktivatsiya qiladi.
     */
    @Transactional
    public String deactivate(String token) {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        deviceTokenRepository.findByProfileIdAndToken(profileId, token.trim())
                .ifPresent(entity -> {
                    entity.setActive(false);
                    deviceTokenRepository.save(entity);
                });

        return "Device token o'chirildi";
    }
}
