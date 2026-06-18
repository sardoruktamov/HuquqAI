package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.lawyer.*;
import api.ailawyer.uz.entity.LawyerProfileEntity;
import api.ailawyer.uz.entity.ProfileEntity;
import api.ailawyer.uz.enums.GeneralStatus;
import api.ailawyer.uz.enums.LawyerOnboardingStatus;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerProfileRepository;
import api.ailawyer.uz.repository.ProfileRepository;
import api.ailawyer.uz.repository.ProfileRoleRepository;
import api.ailawyer.uz.service.mapper.LawyerPublicMapper;
import api.ailawyer.uz.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advokat profili bilan bog'liq barcha biznes logika shu servisda.
 * <p>
 * Vazifalari:
 * <ul>
 *   <li>Onboarding — advokat ma'lumotlarini to'ldirish (DRAFT)</li>
 *   <li>Admin tasdiqlash/rad etish (PENDING → APPROVED/REJECTED)</li>
 *   <li>Public katalog — tasdiqlangan advokatlarni qidirish va ko'rsatish</li>
 *   <li>Chat boshlashdan oldin advokat tasdiqlanganligini tekshirish</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LawyerProfileService {

    /** Advokat profili jadvali bilan ishlash */
    private final LawyerProfileRepository lawyerProfileRepository;
    /** Foydalanuvchi (profile) jadvali */
    private final ProfileRepository profileRepository;
    /** Foydalanuvchi rollari jadvali */
    private final ProfileRoleRepository profileRoleRepository;
    /** ROLE_LAWYER qo'shish uchun */
    private final ProfileRoleService profileRoleService;
    /** Rasm va litsenziya fayllarini DTO ga aylantirish */
    private final AttachService attachService;
    /** Push bildirishnomalar (asinxron event) */
    private final NotificationService notificationService;
    /** Harakatlar tarixi (audit log) */
    private final AuditLogService auditLogService;

    private static final String AUDIT_ENTITY_LAWYER_PROFILE = "lawyer_profile";
    private static final String AUDIT_ACTION_APPROVE = "APPROVE";
    private static final String AUDIT_ACTION_REJECT = "REJECT";

    /**
     * Advokat onboarding — birinchi marta yoki qayta ma'lumot to'ldirish.
     * <p>
     * Agar foydalanuvchida ROLE_LAWYER yo'q bo'lsa, avtomatik qo'shiladi.
     * Holat DRAFT ga o'rnatiladi. PENDING yoki APPROVED holatda tahrirlash mumkin emas.
     *
     * @param dto advokat kiritgan ma'lumotlar
     * @return yangilangan profil DTO
     */
    @Transactional
    public LawyerProfileDTO onboarding(LawyerOnboardingDTO dto) {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(profileId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        if (profile.getStatus() != GeneralStatus.ACTIVE) {
            throw new AppBadException("Profil faol emas!");
        }

        validateLicenseDocument(dto.getLicenseDocumentId());

        if (!profileRoleRepository.existsByProfileIdAndRoles(profileId, ProfileRole.ROLE_LAWYER)) {
            profileRoleService.create(profileId, ProfileRole.ROLE_LAWYER);
        }

        LawyerProfileEntity entity = lawyerProfileRepository.findByProfileId(profileId)
                .orElseGet(() -> createDraft(profileId));

        if (entity.getOnboardingStatus() == LawyerOnboardingStatus.PENDING) {
            throw new AppBadException("Profil tasdiqlash jarayonida. Tahrirlash mumkin emas!");
        }
        if (entity.getOnboardingStatus() == LawyerOnboardingStatus.APPROVED) {
            throw new AppBadException("Profil allaqachon tasdiqlangan. Yangilash uchun profile endpointidan foydalaning.");
        }

        applyOnboarding(entity, dto);
        entity.setOnboardingStatus(LawyerOnboardingStatus.DRAFT);
        entity.setRejectionReason(null);
        entity.setUpdatedDate(LocalDateTime.now());
        lawyerProfileRepository.save(entity);

        return toProfileDto(entity, profile);
    }

    /**
     * Tasdiqlangan advokat o'z profilini qisman yangilaydi.
     * PENDING holatda tahrirlash taqiqlanadi.
     *
     * @param dto yangilanadigan maydonlar (null bo'lsa o'zgartirilmaydi)
     */
    @Transactional
    public LawyerProfileDTO updateProfile(LawyerProfileUpdateDTO dto) {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();
        requireLawyerRole(profileId);

        LawyerProfileEntity entity = getEntityByProfileId(profileId);
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(profileId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        if (entity.getOnboardingStatus() == LawyerOnboardingStatus.PENDING) {
            throw new AppBadException("Profil tasdiqlash jarayonida. Tahrirlash mumkin emas!");
        }

        if (dto.getSpecializations() != null) {
            entity.setSpecializations(normalizeListInput(dto.getSpecializations()));
        }
        if (dto.getExperienceYears() != null) {
            entity.setExperienceYears(dto.getExperienceYears());
        }
        if (dto.getBio() != null) {
            entity.setBio(dto.getBio().trim());
        }
        if (dto.getLicenseNumber() != null) {
            entity.setLicenseNumber(dto.getLicenseNumber().trim());
        }
        if (dto.getLicenseDocumentId() != null) {
            validateLicenseDocument(dto.getLicenseDocumentId());
            entity.setLicenseDocumentId(dto.getLicenseDocumentId());
        }
        if (dto.getRegion() != null) {
            entity.setRegion(dto.getRegion().trim());
        }
        if (dto.getLanguages() != null) {
            entity.setLanguages(joinList(dto.getLanguages()));
        }
        if (dto.getIsAvailable() != null) {
            entity.setIsAvailable(dto.getIsAvailable());
        }

        entity.setUpdatedDate(LocalDateTime.now());
        lawyerProfileRepository.save(entity);

        return toProfileDto(entity, profile);
    }

    /**
     * Joriy kirgan advokat o'z profilini ko'radi.
     *
     * @return to'liq advokat profili (onboarding holati, rad sababi va hokazo)
     */
    public LawyerProfileDTO getMe() {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();
        requireLawyerRole(profileId);

        LawyerProfileEntity entity = lawyerProfileRepository.findByProfileId(profileId)
                .orElseThrow(() -> new AppBadException("Advokat profili topilmadi! Avval onboarding qiling."));
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(profileId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        return toProfileDto(entity, profile);
    }

    /**
     * Advokat profilni admin tasdiqlashiga yuboradi (DRAFT/REJECTED → PENDING).
     * Barcha majburiy maydonlar to'ldirilgan bo'lishi kerak.
     */
    @Transactional
    public LawyerProfileDTO submitOnboarding() {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();
        requireLawyerRole(profileId);

        LawyerProfileEntity entity = getEntityByProfileId(profileId);
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(profileId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        if (entity.getOnboardingStatus() != LawyerOnboardingStatus.DRAFT
                && entity.getOnboardingStatus() != LawyerOnboardingStatus.REJECTED) {
            throw new AppBadException("Faqat DRAFT yoki REJECTED holatdagi profil yuborilishi mumkin!");
        }

        validateRequiredForSubmit(entity);

        entity.setOnboardingStatus(LawyerOnboardingStatus.PENDING);
        entity.setRejectionReason(null);
        entity.setUpdatedDate(LocalDateTime.now());
        lawyerProfileRepository.save(entity);

        notificationService.notifyLawyerOnboardingPending(profileId);

        return toProfileDto(entity, profile);
    }

    /**
     * Tasdiqlangan advokatlar katalogini filter bilan qaytaradi.
     * Litsenziya ma'lumotlari bu ro'yxatda ko'rsatilmaydi.
     *
     * @param dto    filter parametrlari (query, region, specialization, minExperience)
     * @param page   sahifa raqami (0-based)
     * @param size   sahifadagi elementlar soni
     */
    public PageImpl<LawyerPublicDTO> publicFilter(LawyerFilterDTO dto, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<LawyerPublicMapper> result = lawyerProfileRepository.findApprovedPublic(
                likeOrNull(dto.getQuery()),
                likeOrNull(dto.getRegion()),
                likeOrNull(dto.getSpecialization()),
                dto.getMinExperience(),
                pageRequest
        );

        List<LawyerPublicDTO> list = result.getContent().stream()
                .map(this::toPublicDto)
                .toList();

        return new PageImpl<>(list, pageRequest, result.getTotalElements());
    }

    /**
     * Bitta tasdiqlangan advokatning to'liq public profilini qaytaradi.
     * Litsenziya raqami va hujjat faqat shu metodda ko'rsatiladi.
     *
     * @param profileId advokat profile id si
     */
    public LawyerPublicDetailDTO publicGetById(Integer profileId) {
        LawyerPublicMapper mapper = lawyerProfileRepository.findApprovedPublicByProfileId(profileId)
                .orElseThrow(() -> new AppBadException("Advokat topilmadi yoki hali tasdiqlanmagan!"));

        return toPublicDetailDto(mapper);
    }

    /**
     * Admin advokat onboarding arizasini tasdiqlaydi (PENDING → APPROVED).
     * verifiedAt vaqt belgilanadi, advokat public katalogda paydo bo'ladi.
     *
     * @param profileId tasdiqlanadigan advokat id si
     */
    @Transactional
    public LawyerProfileDTO approve(Integer profileId) {
        LawyerProfileEntity entity = getEntityByProfileId(profileId);
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(profileId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        if (!profileRoleRepository.existsByProfileIdAndRoles(profileId, ProfileRole.ROLE_LAWYER)) {
            throw new AppBadException("Bu foydalanuvchi advokat emas!");
        }
        if (entity.getOnboardingStatus() != LawyerOnboardingStatus.PENDING) {
            throw new AppBadException("Faqat PENDING holatdagi profil tasdiqlanishi mumkin!");
        }

        entity.setOnboardingStatus(LawyerOnboardingStatus.APPROVED);
        entity.setVerifiedAt(LocalDateTime.now());
        entity.setRejectionReason(null);
        entity.setUpdatedDate(LocalDateTime.now());
        lawyerProfileRepository.save(entity);

        auditLogService.logAction(
                AUDIT_ENTITY_LAWYER_PROFILE,
                String.valueOf(profileId),
                AUDIT_ACTION_APPROVE,
                SpringSecurityUtil.getCurrentUserId(),
                null
        );

        notificationService.notifyLawyerOnboardingApproved(profileId);

        return toProfileDto(entity, profile);
    }

    /**
     * Admin advokat arizasini rad etadi (PENDING → REJECTED).
     * Rad sababi rejectionReason ga yoziladi.
     *
     * @param profileId rad etiladigan advokat id si
     * @param dto       rad etish sababi
     */
    @Transactional
    public LawyerProfileDTO reject(Integer profileId, LawyerRejectDTO dto) {
        LawyerProfileEntity entity = getEntityByProfileId(profileId);
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(profileId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        if (entity.getOnboardingStatus() != LawyerOnboardingStatus.PENDING) {
            throw new AppBadException("Faqat PENDING holatdagi profil rad etilishi mumkin!");
        }

        String rejectionReason = dto.getReason().trim();

        entity.setOnboardingStatus(LawyerOnboardingStatus.REJECTED);
        entity.setRejectionReason(rejectionReason);
        entity.setVerifiedAt(null);
        entity.setUpdatedDate(LocalDateTime.now());
        lawyerProfileRepository.save(entity);

        auditLogService.logAction(
                AUDIT_ENTITY_LAWYER_PROFILE,
                String.valueOf(profileId),
                AUDIT_ACTION_REJECT,
                SpringSecurityUtil.getCurrentUserId(),
                rejectionReason
        );

        notificationService.notifyLawyerOnboardingRejected(profileId, rejectionReason);

        return toProfileDto(entity, profile);
    }

    /**
     * Admin uchun PENDING holatdagi barcha arizalar ro'yxati.
     */
    public PageImpl<LawyerProfileDTO> listPending(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<LawyerProfileEntity> pending = lawyerProfileRepository
                .findAllByOnboardingStatusOrderByUpdatedDateDesc(LawyerOnboardingStatus.PENDING, pageRequest);

        List<LawyerProfileDTO> list = pending.getContent().stream()
                .map(entity -> {
                    ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(entity.getProfileId())
                            .orElse(null);
                    return toProfileDto(entity, profile);
                })
                .toList();

        return new PageImpl<>(list, pageRequest, pending.getTotalElements());
    }

    /**
     * Advokat bilan chat boshlashdan oldin tekshiruv.
     * LawyerMessageService chaqiradi — advokat faol, ROLE_LAWYER va APPROVED bo'lishi shart.
     *
     * @param lawyerProfileId chat boshlanadigan advokat id si
     * @throws AppBadException advokat topilmasa yoki tasdiqlanmagan bo'lsa
     */
    public void requireApprovedLawyer(Integer lawyerProfileId) {
        ProfileEntity profile = profileRepository.findByIdAndVisibleTrue(lawyerProfileId)
                .orElseThrow(() -> new AppBadException("Advokat topilmadi!"));

        if (profile.getStatus() != GeneralStatus.ACTIVE) {
            throw new AppBadException("Advokat profili faol emas!");
        }
        if (!profileRoleRepository.existsByProfileIdAndRoles(lawyerProfileId, ProfileRole.ROLE_LAWYER)) {
            throw new AppBadException("Tanlangan foydalanuvchi advokat emas!");
        }

        LawyerProfileEntity lawyerProfile = lawyerProfileRepository.findByProfileId(lawyerProfileId)
                .orElseThrow(() -> new AppBadException("Advokat profili to'ldirilmagan!"));

        if (lawyerProfile.getOnboardingStatus() != LawyerOnboardingStatus.APPROVED) {
            throw new AppBadException("Advokat hali tasdiqlanmagan!");
        }
    }

    /** Yangi DRAFT profil yaratadi (birinchi onboarding chaqiruvida) */
    private LawyerProfileEntity createDraft(Integer profileId) {
        LawyerProfileEntity entity = new LawyerProfileEntity();
        entity.setProfileId(profileId);
        entity.setOnboardingStatus(LawyerOnboardingStatus.DRAFT);
        entity.setIsAvailable(true);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setUpdatedDate(LocalDateTime.now());
        return lawyerProfileRepository.save(entity);
    }

    /** Profile id bo'yicha advokat entity sini topadi, topilmasa xato */
    private LawyerProfileEntity getEntityByProfileId(Integer profileId) {
        return lawyerProfileRepository.findByProfileId(profileId)
                .orElseThrow(() -> new AppBadException("Advokat profili topilmadi!"));
    }

    /** Foydalanuvchida ROLE_LAWYER borligini tekshiradi */
    private void requireLawyerRole(Integer profileId) {
        if (!profileRoleRepository.existsByProfileIdAndRoles(profileId, ProfileRole.ROLE_LAWYER)) {
            throw new AppBadException("Sizda advokat roli yo'q!");
        }
    }

    /** Onboarding DTO dagi ma'lumotlarni entity ga yozadi */
    private void applyOnboarding(LawyerProfileEntity entity, LawyerOnboardingDTO dto) {
        entity.setSpecializations(normalizeListInput(dto.getSpecializations()));
        entity.setExperienceYears(dto.getExperienceYears());
        entity.setBio(dto.getBio() != null ? dto.getBio().trim() : null);
        entity.setLicenseNumber(dto.getLicenseNumber().trim());
        entity.setLicenseDocumentId(dto.getLicenseDocumentId());
        entity.setRegion(dto.getRegion().trim());
        entity.setLanguages(joinList(dto.getLanguages()));
    }

    /** Submit qilishdan oldin majburiy maydonlar to'ldirilganini tekshiradi */
    private void validateRequiredForSubmit(LawyerProfileEntity entity) {
        if (entity.getSpecializations() == null || entity.getSpecializations().isBlank()) {
            throw new AppBadException("Ixtisoslik to'ldirilmagan!");
        }
        if (entity.getExperienceYears() == null) {
            throw new AppBadException("Tajriba yili to'ldirilmagan!");
        }
        if (entity.getLicenseNumber() == null || entity.getLicenseNumber().isBlank()) {
            throw new AppBadException("Litsenziya raqami to'ldirilmagan!");
        }
        if (entity.getLicenseDocumentId() == null || entity.getLicenseDocumentId().isBlank()) {
            throw new AppBadException("Litsenziya hujjati yuklanmagan!");
        }
        if (entity.getRegion() == null || entity.getRegion().isBlank()) {
            throw new AppBadException("Hudud to'ldirilmagan!");
        }
    }

    /** Litsenziya fayli attach jadvalida mavjudligini tekshiradi */
    private void validateLicenseDocument(String attachId) {
        attachService.getEntity(attachId);
    }

    /** SQL LIKE qidiruv uchun matnni %...% formatiga o'giradi, bo'sh bo'lsa null */
    private String likeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim().toLowerCase() + "%";
    }

    /** Vergul bilan ajratilgan matnni tozalab, qayta birlashtiradi */
    private String normalizeListInput(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(","));
    }

    /** String ro'yxatni vergul bilan bir qator matnga aylantiradi */
    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(","));
    }

    /** Vergul bilan saqlangan matnni String ro'yxatga ajratadi */
    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    /** SQL projection ni public ro'yxat DTO ga aylantiradi (litsenziyasiz) */
    private LawyerPublicDTO toPublicDto(LawyerPublicMapper mapper) {
        LawyerPublicDTO dto = new LawyerPublicDTO();
        dto.setId(mapper.getId());
        dto.setFullName(mapper.getFullName());
        dto.setPhoto(attachService.attachDTO(mapper.getPhotoId()));
        dto.setSpecializations(splitList(mapper.getSpecializations()));
        dto.setExperienceYears(mapper.getExperienceYears());
        dto.setRegion(mapper.getRegion());
        dto.setLanguages(splitList(mapper.getLanguages()));
        dto.setIsAvailable(mapper.getIsAvailable());
        dto.setPostCount(mapper.getPostCount());
        dto.setVerifiedAt(mapper.getVerifiedAt());
        return dto;
    }

    /** SQL projection ni to'liq public detail DTO ga aylantiradi (litsenziya bilan) */
    private LawyerPublicDetailDTO toPublicDetailDto(LawyerPublicMapper mapper) {
        LawyerPublicDetailDTO dto = new LawyerPublicDetailDTO();
        dto.setId(mapper.getId());
        dto.setFullName(mapper.getFullName());
        dto.setPhoto(attachService.attachDTO(mapper.getPhotoId()));
        dto.setSpecializations(splitList(mapper.getSpecializations()));
        dto.setExperienceYears(mapper.getExperienceYears());
        dto.setRegion(mapper.getRegion());
        dto.setLanguages(splitList(mapper.getLanguages()));
        dto.setIsAvailable(mapper.getIsAvailable());
        dto.setPostCount(mapper.getPostCount());
        dto.setVerifiedAt(mapper.getVerifiedAt());
        dto.setBio(mapper.getBio());
        dto.setLicenseNumber(mapper.getLicenseNumber());
        dto.setLicenseDocument(attachService.attachDTO(mapper.getLicenseDocumentId()));
        return dto;
    }

    /** Entity va Profile ni advokat profil DTO ga aylantiradi (advokat o'zi yoki admin ko'radi) */
    private LawyerProfileDTO toProfileDto(LawyerProfileEntity entity, ProfileEntity profile) {
        LawyerProfileDTO dto = new LawyerProfileDTO();
        dto.setProfileId(entity.getProfileId());
        if (profile != null) {
            dto.setFullName(profile.getFullName());
            dto.setPhoto(attachService.attachDTO(profile.getPhotoId()));
        }
        dto.setSpecializations(splitList(entity.getSpecializations()));
        dto.setExperienceYears(entity.getExperienceYears());
        dto.setBio(entity.getBio());
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setLicenseDocument(attachService.attachDTO(entity.getLicenseDocumentId()));
        dto.setOnboardingStatus(entity.getOnboardingStatus());
        dto.setRegion(entity.getRegion());
        dto.setLanguages(splitList(entity.getLanguages()));
        dto.setIsAvailable(entity.getIsAvailable());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setVerifiedAt(entity.getVerifiedAt());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }
}
