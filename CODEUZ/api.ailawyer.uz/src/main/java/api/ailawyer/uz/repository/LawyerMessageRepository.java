package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerMessageEntity;
import api.ailawyer.uz.enums.LawyerMessageSenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link LawyerMessageEntity} bilan ishlash uchun ma'lumotlar bazasi qatlami.
 * <p>
 * Chat ichidagi xabarlarni saqlash, tarix olish va oxirgi xabar preview.
 */
@Repository
public interface LawyerMessageRepository extends JpaRepository<LawyerMessageEntity, UUID> {

    /**
     * Bitta chatdagi barcha xabarlarni yangi dan eskiga qarab qaytaradi.
     *
     * @param lawyerChatId chat UUID si
     * @param pageable     sahifalash
     */
    Page<LawyerMessageEntity> findAllByLawyerChatIdOrderByCreatedDateDesc(UUID lawyerChatId, Pageable pageable);

    /**
     * Bitta chatning eng oxirgi xabarini qaytaradi.
     * Chat ro'yxatida preview ko'rsatish uchun ishlatiladi.
     *
     * @param lawyerChatId chat UUID si
     */
    Optional<LawyerMessageEntity> findFirstByLawyerChatIdOrderByCreatedDateDesc(UUID lawyerChatId);

    /**
     * Bir nechta chat uchun barcha xabarlarni yuklaydi (created_date bo'yicha kamayish).
     * Service ichida har chat uchun birinchi xabar olinadi — N+1 so'rovdan qochish uchun.
     *
     * @param lawyerChatIds chat UUID lar ro'yxati
     */
    List<LawyerMessageEntity> findAllByLawyerChatIdInOrderByCreatedDateDesc(List<UUID> lawyerChatIds);

    /**
     * Qarama-qarshi tomondan kelgan o'qilmagan xabarlar soni.
     *
     * @param afterCreatedDate null bo'lsa — barcha qarama-qarshi xabarlar sanaladi
     */
    @Query("""
            SELECT COUNT(m) FROM LawyerMessageEntity m
            WHERE m.lawyerChatId = :chatId
            AND m.senderType = :senderType
            AND (:afterCreatedDate IS NULL OR m.createdDate > :afterCreatedDate)
            """)
    long countUnreadMessages(
            @Param("chatId") UUID chatId,
            @Param("senderType") LawyerMessageSenderType senderType,
            @Param("afterCreatedDate") LocalDateTime afterCreatedDate
    );
}
