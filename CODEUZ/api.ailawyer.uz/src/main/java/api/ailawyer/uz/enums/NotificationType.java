package api.ailawyer.uz.enums;

/**
 * FCM push bildirishnoma turlari.
 */
public enum NotificationType {

    /** Mijoz advokatga xabar yozganda — advokatga */
    LAWYER_NEW_MESSAGE,

    /** Advokat mijozga javob berganda — mijozga */
    CLIENT_NEW_MESSAGE,

    /** Advokat onboarding submit qilganda — adminlarga */
    LAWYER_ONBOARDING_PENDING,

    /** Admin advokatni tasdiqlaganda — advokatga */
    LAWYER_ONBOARDING_APPROVED,

    /** Admin advokatni rad etganda — advokatga */
    LAWYER_ONBOARDING_REJECTED
}
