package api.ailawyer.uz.service;

import api.ailawyer.uz.dto.lawyer.LawyerRejectDTO;
import api.ailawyer.uz.entity.LawyerProfileEntity;
import api.ailawyer.uz.entity.ProfileEntity;
import api.ailawyer.uz.enums.GeneralStatus;
import api.ailawyer.uz.enums.LawyerOnboardingStatus;
import api.ailawyer.uz.enums.ProfileRole;
import api.ailawyer.uz.exps.AppBadException;
import api.ailawyer.uz.repository.LawyerProfileRepository;
import api.ailawyer.uz.repository.ProfileRepository;
import api.ailawyer.uz.repository.ProfileRoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LawyerProfileServiceTest {

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileRoleRepository profileRoleRepository;
    @Mock
    private ProfileRoleService profileRoleService;
    @Mock
    private AttachService attachService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LawyerProfileService lawyerProfileService;

    @BeforeEach
    void setUp() {
        TestSecurityHelper.loginAs(7, ProfileRole.ROLE_LAWYER);
    }

    @AfterEach
    void tearDown() {
        TestSecurityHelper.clear();
    }

    @Test
    void approve_throwsWhenNotPending() {
        Integer profileId = 7;
        LawyerProfileEntity entity = new LawyerProfileEntity();
        entity.setProfileId(profileId);
        entity.setOnboardingStatus(LawyerOnboardingStatus.DRAFT);

        ProfileEntity profile = new ProfileEntity();
        profile.setId(profileId);
        profile.setStatus(GeneralStatus.ACTIVE);
        profile.setVisible(true);

        when(lawyerProfileRepository.findByProfileId(profileId)).thenReturn(Optional.of(entity));
        when(profileRepository.findByIdAndVisibleTrue(profileId)).thenReturn(Optional.of(profile));
        when(profileRoleRepository.existsByProfileIdAndRoles(profileId, ProfileRole.ROLE_LAWYER)).thenReturn(true);

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerProfileService.approve(profileId));

        assertEquals("Faqat PENDING holatdagi profil tasdiqlanishi mumkin!", ex.getMessage());
    }

    @Test
    void approve_pending_logsAuditAction() {
        TestSecurityHelper.loginAs(1, ProfileRole.ROLE_ADMIN);

        Integer profileId = 7;
        LawyerProfileEntity entity = new LawyerProfileEntity();
        entity.setProfileId(profileId);
        entity.setOnboardingStatus(LawyerOnboardingStatus.PENDING);

        ProfileEntity profile = new ProfileEntity();
        profile.setId(profileId);
        profile.setStatus(GeneralStatus.ACTIVE);
        profile.setVisible(true);

        when(lawyerProfileRepository.findByProfileId(profileId)).thenReturn(Optional.of(entity));
        when(profileRepository.findByIdAndVisibleTrue(profileId)).thenReturn(Optional.of(profile));
        when(profileRoleRepository.existsByProfileIdAndRoles(profileId, ProfileRole.ROLE_LAWYER)).thenReturn(true);
        when(lawyerProfileRepository.save(any(LawyerProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lawyerProfileService.approve(profileId);

        verify(auditLogService).logAction("lawyer_profile", "7", "APPROVE", 1, null);
        verify(notificationService).notifyLawyerOnboardingApproved(profileId);
    }

    @Test
    void reject_pending_logsAuditActionWithReason() {
        TestSecurityHelper.loginAs(2, ProfileRole.ROLE_SUPERADMIN);

        Integer profileId = 7;
        LawyerProfileEntity entity = new LawyerProfileEntity();
        entity.setProfileId(profileId);
        entity.setOnboardingStatus(LawyerOnboardingStatus.PENDING);

        ProfileEntity profile = new ProfileEntity();
        profile.setId(profileId);
        profile.setStatus(GeneralStatus.ACTIVE);
        profile.setVisible(true);

        LawyerRejectDTO rejectDto = new LawyerRejectDTO();
        rejectDto.setReason("Hujjat xato");

        when(lawyerProfileRepository.findByProfileId(profileId)).thenReturn(Optional.of(entity));
        when(profileRepository.findByIdAndVisibleTrue(profileId)).thenReturn(Optional.of(profile));
        when(lawyerProfileRepository.save(any(LawyerProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lawyerProfileService.reject(profileId, rejectDto);

        verify(auditLogService).logAction("lawyer_profile", "7", "REJECT", 2, "Hujjat xato");
        verify(notificationService).notifyLawyerOnboardingRejected(profileId, "Hujjat xato");
    }

    @Test
    void submitOnboarding_throwsWhenPending() {
        Integer profileId = 7;
        LawyerProfileEntity entity = new LawyerProfileEntity();
        entity.setProfileId(profileId);
        entity.setOnboardingStatus(LawyerOnboardingStatus.PENDING);

        ProfileEntity profile = new ProfileEntity();
        profile.setId(profileId);
        profile.setStatus(GeneralStatus.ACTIVE);
        profile.setVisible(true);

        when(profileRoleRepository.existsByProfileIdAndRoles(profileId, ProfileRole.ROLE_LAWYER)).thenReturn(true);
        when(lawyerProfileRepository.findByProfileId(profileId)).thenReturn(Optional.of(entity));
        when(profileRepository.findByIdAndVisibleTrue(profileId)).thenReturn(Optional.of(profile));

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerProfileService.submitOnboarding());

        assertEquals("Faqat DRAFT yoki REJECTED holatdagi profil yuborilishi mumkin!", ex.getMessage());
    }

    @Test
    void requireApprovedLawyer_throwsWhenNotApproved() {
        Integer lawyerId = 5;
        ProfileEntity profile = new ProfileEntity();
        profile.setId(lawyerId);
        profile.setStatus(GeneralStatus.ACTIVE);
        profile.setVisible(true);

        LawyerProfileEntity lawyerProfile = new LawyerProfileEntity();
        lawyerProfile.setProfileId(lawyerId);
        lawyerProfile.setOnboardingStatus(LawyerOnboardingStatus.PENDING);

        when(profileRepository.findByIdAndVisibleTrue(lawyerId)).thenReturn(Optional.of(profile));
        when(profileRoleRepository.existsByProfileIdAndRoles(lawyerId, ProfileRole.ROLE_LAWYER)).thenReturn(true);
        when(lawyerProfileRepository.findByProfileId(lawyerId)).thenReturn(Optional.of(lawyerProfile));

        AppBadException ex = assertThrows(AppBadException.class,
                () -> lawyerProfileService.requireApprovedLawyer(lawyerId));

        assertEquals("Advokat hali tasdiqlanmagan!", ex.getMessage());
    }
}
