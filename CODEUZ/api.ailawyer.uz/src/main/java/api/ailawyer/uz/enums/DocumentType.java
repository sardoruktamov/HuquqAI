package api.ailawyer.uz.enums;

/**
 * Huquqiy hujjat turi (Lex.uz tasnifiga mos).
 */
public enum DocumentType {

    /** Kodeks (masalan, Mehnat kodeksi) */
    CODE,

    /** Qonun */
    LAW,

    /** Prezident farmoni */
    PRESIDENTIAL_DECREE,

    /** Prezident qarori */
    PRESIDENTIAL_RESOLUTION,

    /** Vazirlar Mahkamasi qarori */
    CABINET_RESOLUTION,

    /** Vazirlik buyrug'i */
    MINISTRY_ORDER,

    /** Boshqa hujjat turlari */
    OTHER
}
