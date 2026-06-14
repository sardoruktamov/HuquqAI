package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerProfileEntity;
import api.ailawyer.uz.enums.LawyerOnboardingStatus;
import api.ailawyer.uz.service.mapper.LawyerPublicMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * {@link LawyerProfileEntity} bilan ishlash uchun ma'lumotlar bazasi qatlami.
 * <p>
 * Bu repository advokat profilini saqlash, qidirish va public katalog uchun
 * murakkab SQL so'rovlarni bajaradi.
 */
public interface LawyerProfileRepository extends CrudRepository<LawyerProfileEntity, Integer>,
        PagingAndSortingRepository<LawyerProfileEntity, Integer> {

    /**
     * Profile id bo'yicha advokat profilini topadi.
     *
     * @param profileId foydalanuvchi (profile) id si
     */
    Optional<LawyerProfileEntity> findByProfileId(Integer profileId);

    /**
     * Berilgan profile id uchun advokat profili mavjudligini tekshiradi.
     *
     * @param profileId foydalanuvchi id si
     * @return true — profil bor, false — yo'q
     */
    boolean existsByProfileId(Integer profileId);

    /**
     * Tasdiqlangan (APPROVED) advokatlar ro'yxatini filter bilan qaytaradi.
     * <p>
     * Faqat quyidagilar qaytadi:
     * - profile visible=true va status=ACTIVE
     * - ROLE_LAWYER roli bor
     * - onboarding_status=APPROVED
     *
     * @param query          ism, ixtisoslik yoki hudud bo'yicha qidiruv (LIKE)
     * @param region         hudud filteri
     * @param specialization ixtisoslik filteri
     * @param minExperience  minimal tajriba yili
     * @param pageable       sahifalash
     */
    @Query(value = """
            SELECT p.id AS id,
                   p.name AS fullName,
                   p.photo_id AS photoId,
                   lp.specializations AS specializations,
                   lp.experience_years AS experienceYears,
                   lp.region AS region,
                   lp.languages AS languages,
                   lp.bio AS bio,
                   lp.license_number AS licenseNumber,
                   lp.license_document_id AS licenseDocumentId,
                   lp.onboarding_status AS onboardingStatus,
                   lp.is_available AS isAvailable,
                   lp.verified_at AS verifiedAt,
                   (SELECT COUNT(*) FROM post pt WHERE pt.profile_id = p.id) AS postCount
            FROM profile p
            INNER JOIN lawyer_profile lp ON lp.profile_id = p.id
            INNER JOIN profile_role pr ON pr.profile_id = p.id AND pr.roles = 'ROLE_LAWYER'
            WHERE p.visible IS TRUE
              AND p.status = 'ACTIVE'
              AND lp.onboarding_status = 'APPROVED'
              AND (:query IS NULL OR lower(p.name) LIKE :query OR lower(lp.specializations) LIKE :query OR lower(lp.region) LIKE :query)
              AND (:region IS NULL OR lower(lp.region) LIKE :region)
              AND (:specialization IS NULL OR lower(lp.specializations) LIKE :specialization)
              AND (:minExperience IS NULL OR lp.experience_years >= :minExperience)
            ORDER BY lp.verified_at DESC NULLS LAST, p.created_date DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM profile p
            INNER JOIN lawyer_profile lp ON lp.profile_id = p.id
            INNER JOIN profile_role pr ON pr.profile_id = p.id AND pr.roles = 'ROLE_LAWYER'
            WHERE p.visible IS TRUE
              AND p.status = 'ACTIVE'
              AND lp.onboarding_status = 'APPROVED'
              AND (:query IS NULL OR lower(p.name) LIKE :query OR lower(lp.specializations) LIKE :query OR lower(lp.region) LIKE :query)
              AND (:region IS NULL OR lower(lp.region) LIKE :region)
              AND (:specialization IS NULL OR lower(lp.specializations) LIKE :specialization)
              AND (:minExperience IS NULL OR lp.experience_years >= :minExperience)
            """,
            nativeQuery = true)
    Page<LawyerPublicMapper> findApprovedPublic(
            @Param("query") String query,
            @Param("region") String region,
            @Param("specialization") String specialization,
            @Param("minExperience") Integer minExperience,
            Pageable pageable
    );

    /**
     * Bitta tasdiqlangan advokatning to'liq public ma'lumotini qaytaradi.
     * Litsenziya raqami va hujjat id si ham shu so'rovda keladi.
     *
     * @param profileId advokat profile id si
     */
    @Query(value = """
            SELECT p.id AS id,
                   p.name AS fullName,
                   p.photo_id AS photoId,
                   lp.specializations AS specializations,
                   lp.experience_years AS experienceYears,
                   lp.region AS region,
                   lp.languages AS languages,
                   lp.bio AS bio,
                   lp.license_number AS licenseNumber,
                   lp.license_document_id AS licenseDocumentId,
                   lp.onboarding_status AS onboardingStatus,
                   lp.is_available AS isAvailable,
                   lp.verified_at AS verifiedAt,
                   (SELECT COUNT(*) FROM post pt WHERE pt.profile_id = p.id) AS postCount
            FROM profile p
            INNER JOIN lawyer_profile lp ON lp.profile_id = p.id
            INNER JOIN profile_role pr ON pr.profile_id = p.id AND pr.roles = 'ROLE_LAWYER'
            WHERE p.id = :profileId
              AND p.visible IS TRUE
              AND p.status = 'ACTIVE'
              AND lp.onboarding_status = 'APPROVED'
            """,
            nativeQuery = true)
    Optional<LawyerPublicMapper> findApprovedPublicByProfileId(@Param("profileId") Integer profileId);

    /**
     * Admin uchun: berilgan onboarding holatidagi profillar ro'yxati.
     * Odatda PENDING holatidagi arizalar olinadi.
     *
     * @param onboardingStatus qidiriladigan holat (masalan PENDING)
     * @param pageable         sahifalash
     */
    Page<LawyerProfileEntity> findAllByOnboardingStatusOrderByUpdatedDateDesc(
            LawyerOnboardingStatus onboardingStatus,
            Pageable pageable
    );
}
