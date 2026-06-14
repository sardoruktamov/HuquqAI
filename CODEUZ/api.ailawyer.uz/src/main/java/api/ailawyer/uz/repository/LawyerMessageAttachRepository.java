package api.ailawyer.uz.repository;

import api.ailawyer.uz.entity.LawyerMessageAttachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * {@link LawyerMessageAttachEntity} bilan ishlash uchun ma'lumotlar bazasi qatlami.
 * <p>
 * Xabarga biriktirilgan fayllarni saqlash va olish.
 */
@Repository
public interface LawyerMessageAttachRepository extends JpaRepository<LawyerMessageAttachEntity, UUID> {

    /**
     * Bitta xabarga biriktirilgan barcha fayllarni qaytaradi.
     *
     * @param messageId xabar UUID si
     */
    List<LawyerMessageAttachEntity> findAllByMessageId(UUID messageId);

    /**
     * Bir nechta xabarga biriktirilgan fayllarni batch yuklaydi.
     * Xabarlar ro'yxatini ko'rsatishda N+1 dan qochish uchun ishlatiladi.
     *
     * @param messageIds xabar UUID lar ro'yxati
     */
    List<LawyerMessageAttachEntity> findAllByMessageIdIn(List<UUID> messageIds);
}
