package api.ailawyer.uz.enums;

/**
 * Advokat chatining holati.
 * <p>
 * {@link api.ailawyer.uz.entity.LawyerChatEntity#status} maydonida saqlanadi.
 */
public enum LawyerChatStatus {

    /** Faol — ikkala tomon xabar almashishi mumkin */
    ACTIVE,

    /** Yopilgan — yangi xabar yozib bo'lmaydi, faqat tarix ko'rinadi */
    CLOSED
}
