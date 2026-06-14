package api.ailawyer.uz.dto.lawyer;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.enums.LawyerOnboardingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Advokatning to'liq profil ma'lumotlari — advokat o'zi yoki admin ko'radi.
 * <p>
 * /me, onboarding, approve/reject va pending endpointlarida qaytariladi.
 * Public katalog DTO sidan farqi: onboarding holati, rad sababi, litsenziya va barcha ichki maydonlar bor.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LawyerProfileDTO {

    /** Foydalanuvchi (profile) id si */
    private Integer profileId;

    /** Advokat to'liq ismi */
    private String fullName;

    /** Profil rasmi */
    private AttachDTO photo;

    /** Ixtisosliklar ro'yxati */
    private List<String> specializations;

    /** Tajriba yili */
    private Integer experienceYears;

    /** Bio matni */
    private String bio;

    /** Litsenziya raqami */
    private String licenseNumber;

    /** Litsenziya hujjati fayli */
    private AttachDTO licenseDocument;

    /** Onboarding holati: DRAFT, PENDING, APPROVED, REJECTED */
    private LawyerOnboardingStatus onboardingStatus;

    /** Hudud/shahar */
    private String region;

    /** Tilllar ro'yxati */
    private List<String> languages;

    /** Yangi mijoz qabul qiladimi */
    private Boolean isAvailable;

    /** Admin rad etganda yozilgan sabab (REJECTED holatda) */
    private String rejectionReason;

    /** Admin tasdiqlagan vaqt (APPROVED holatda) */
    private LocalDateTime verifiedAt;

    /** Profil yaratilgan vaqt */
    private LocalDateTime createdDate;

    /** Profil oxirgi yangilangan vaqt */
    private LocalDateTime updatedDate;
}
