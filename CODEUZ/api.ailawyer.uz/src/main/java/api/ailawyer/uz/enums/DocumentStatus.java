package api.ailawyer.uz.enums;

/**
 * Huquqiy hujjatning amaldagi holati (RAG qidiruv filtri uchun).
 */
public enum DocumentStatus {

    /** Hujjat amalda, AI maslahatlarida ishlatiladi */
    ACTIVE,

    /** Hujjat qisman o'zgartirilgan (ba'zi moddalar yangilangan) */
    PARTIALLY_AMENDED,

    /** Hujjat yangi hujjat bilan almashtirilgan */
    SUPERSEDED,

    /** Hujjat bekor qilingan, AI maslahatlarida ishlatilmaydi */
    REVOKED
}
