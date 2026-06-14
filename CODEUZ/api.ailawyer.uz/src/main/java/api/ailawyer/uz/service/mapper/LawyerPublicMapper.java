package api.ailawyer.uz.service.mapper;

import api.ailawyer.uz.enums.LawyerOnboardingStatus;

import java.time.LocalDateTime;

/**
 * Native SQL so'rov natijasini Java obyektiga aylantirish uchun projection interfeysi.
 * <p>
 * Spring Data JPA {@link api.ailawyer.uz.repository.LawyerProfileRepository} dagi
 * native query natijalarini ushbu interfeys orqali qabul qiladi.
 * Har bir getter SQL dagi alias nomiga mos keladi.
 */
public interface LawyerPublicMapper {

    /** Advokatning profile id si */
    Integer getId();

    /** Advokat to'liq ismi */
    String getFullName();

    /** Profil rasmi attach id si */
    String getPhotoId();

    /** Ixtisosliklar (vergul bilan ajratilgan matn) */
    String getSpecializations();

    /** Tajriba yili */
    Integer getExperienceYears();

    /** Hudud/shahar */
    String getRegion();

    /** Tilllar (vergul bilan ajratilgan matn) */
    String getLanguages();

    /** Bio matni (faqat detail so'rovda) */
    String getBio();

    /** Litsenziya raqami (faqat detail so'rovda) */
    String getLicenseNumber();

    /** Litsenziya hujjati attach id si (faqat detail so'rovda) */
    String getLicenseDocumentId();

    /** Onboarding holati */
    LawyerOnboardingStatus getOnboardingStatus();

    /** Mijoz qabul qiladimi */
    Boolean getIsAvailable();

    /** Admin tasdiqlagan vaqt */
    LocalDateTime getVerifiedAt();

    /** Advokatning postlar soni */
    Long getPostCount();
}
