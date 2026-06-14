package api.ailawyer.uz.enums;

/**
 * Advokat onboarding (profilni tasdiqlash) jarayonining holatlari.
 * <p>
 * Bu enum {@link api.ailawyer.uz.entity.LawyerProfileEntity#onboardingStatus} maydonida saqlanadi
 * va advokat qaysi bosqichda ekanini ko'rsatadi.
 */
public enum LawyerOnboardingStatus {

    /** Qoralama — advokat ma'lumotlarni to'ldirmoqda, hali admin ko'rmaydi */
    DRAFT,

    /** Yuborilgan — advokat profilni tasdiqlash uchun admin kutmoqda */
    PENDING,

    /** Tasdiqlangan — advokat public katalogda ko'rinadi, mijozlar murojaat qila oladi */
    APPROVED,

    /** Rad etilgan — admin rad qilgan, advokat qayta tahrirlab yuborishi mumkin */
    REJECTED
}
