package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.ProfileRoleEntity;
import api.ailawyer.uz.enums.ProfileRole;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Foydalanuvchi rollari (ROLE_USER, ROLE_LAWYER, ROLE_ADMIN) bilan ishlash.
 */
public interface ProfileRoleRepository extends CrudRepository<ProfileRoleEntity,Integer> {

    /** Foydalanuvchining barcha rollarini o'chiradi */
    @Transactional
    @Modifying
    void deleteByProfileId(Integer integer);

    /** Foydalanuvchining barcha rollarini ro'yxat sifatida qaytaradi */
    @Query("select p.roles from ProfileRoleEntity p where p.profileId = ?1")
    List<ProfileRole> getAllRolesListByProfileId(Integer profileId);

    /**
     * Foydalanuvchida berilgan rol bor-yo'qligini tekshiradi.
     * Masalan: advokat tizimida ROLE_LAWYER borligini tekshirish uchun ishlatiladi.
     *
     * @param profileId foydalanuvchi id si
     * @param roles     qidiriladigan rol (masalan ROLE_LAWYER)
     */
    boolean existsByProfileIdAndRoles(Integer profileId, ProfileRole roles);

    /** Berilgan rollarga ega profil id lar ro'yxati (admin push uchun) */
    @Query("select distinct p.profileId from ProfileRoleEntity p where p.roles in ?1")
    List<Integer> findDistinctProfileIdsByRolesIn(List<ProfileRole> roles);
}
