package api.ailawyer.uz.entity;

import api.ailawyer.uz.enums.LawyerOnboardingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Advokatga xos qo'shimcha profil ma'lumotlari.
 * <p>
 * Oddiy {@link ProfileEntity} faqat ism, login, foto saqlaydi.
 * Bu jadval esa advokatlik faoliyati uchun kerak bo'lgan ma'lumotlarni saqlaydi:
 * ixtisoslik, tajriba, litsenziya, hudud va hokazo.
 * <p>
 * Har bir advokat uchun faqat bitta qator bo'ladi (profile_id unique).
 */
@Entity
@Table(
        name = "lawyer_profile",
        indexes = {
                @Index(name = "idx_lawyer_profile_profile_id", columnList = "profile_id"),
                @Index(name = "idx_lawyer_profile_onboarding_status", columnList = "onboarding_status"),
                @Index(name = "idx_lawyer_profile_region", columnList = "region")
        }
)
@Getter
@Setter
public class LawyerProfileEntity {

    /** Jadvaldagi asosiy kalit (auto increment) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Qaysi foydalanuvchi (profile jadvalidagi id) — advokatning user id si */
    @Column(name = "profile_id", nullable = false, unique = true)
    private Integer profileId;

    /** profile_id ga bog'langan ProfileEntity (faqat o'qish uchun, lazy yuklanadi) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", insertable = false, updatable = false)
    private ProfileEntity profile;

    /** Ixtisosliklar — vergul bilan ajratilgan matn, masalan: "Fuqarolik huquqi,Meros" */
    @Column(name = "specializations", length = 1000)
    private String specializations;

    /** Ish tajribasi (yil hisobida), masalan: 12 */
    @Column(name = "experience_years")
    private Integer experienceYears;

    /** Advokat haqida qisqa tavsif (bio) */
    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    /** Advokatlik litsenziyasi raqami */
    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    /** Litsenziya hujjati fayli id si (attach jadvalidagi id) */
    @Column(name = "license_document_id")
    private String licenseDocumentId;

    /** Litsenziya fayliga bog'lanish (faqat o'qish uchun) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_document_id", insertable = false, updatable = false)
    private AttachEntity licenseDocument;

    /** Onboarding holati: DRAFT, PENDING, APPROVED, REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false)
    private LawyerOnboardingStatus onboardingStatus = LawyerOnboardingStatus.DRAFT;

    /** Advokat faoliyat yuritadigan hudud/shahar, masalan: "Toshkent" */
    @Column(name = "region", length = 120)
    private String region;

    /** Qaysi tillarda xizmat ko'rsatadi — vergul bilan, masalan: "UZ,RU" */
    @Column(name = "languages", length = 255)
    private String languages;

    /** Hozir yangi mijoz qabul qiladimi (true = mavjud, false = band) */
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = Boolean.TRUE;

    /** Admin rad etganda yoziladigan sabab matni */
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    /** Admin tasdiqlagan vaqt (APPROVED bo'lganda to'ldiriladi) */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** Profil qachon yaratilgan */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /** Profil oxirgi marta qachon yangilangan */
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
