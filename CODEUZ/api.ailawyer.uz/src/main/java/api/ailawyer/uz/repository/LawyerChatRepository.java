package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerChatEntity;
import api.ailawyer.uz.enums.LawyerChatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link LawyerChatEntity} bilan ishlash uchun ma'lumotlar bazasi qatlami.
 * <p>
 * Mijoz-advokat chatlarini saqlash, qidirish va ro'yxat olish.
 */
@Repository
public interface LawyerChatRepository extends JpaRepository<LawyerChatEntity, UUID> {

    /**
     * Mijozning barcha chatlarini yangi dan eskiga qarab qaytaradi.
     *
     * @param clientId mijoz profile id si
     * @param pageable sahifalash
     */
    Page<LawyerChatEntity> findAllByClientIdOrderByCreatedDateDesc(Integer clientId, Pageable pageable);

    /**
     * Advokatning barcha chatlarini yangi dan eskiga qarab qaytaradi.
     *
     * @param lawyerId advokat profile id si
     * @param pageable sahifalash
     */
    Page<LawyerChatEntity> findAllByLawyerIdOrderByCreatedDateDesc(Integer lawyerId, Pageable pageable);

    /**
     * Mijoz va advokat o'rtasidagi faol chatni topadi.
     * Chat boshlashda ishlatiladi — mavjud ACTIVE chat bo'lsa qayta yaratilmaydi.
     *
     * @param clientId mijoz id si
     * @param lawyerId advokat id si
     * @param status   qidiriladigan holat (odatda ACTIVE)
     */
    Optional<LawyerChatEntity> findByClientIdAndLawyerIdAndStatus(Integer clientId, Integer lawyerId, LawyerChatStatus status);
}
