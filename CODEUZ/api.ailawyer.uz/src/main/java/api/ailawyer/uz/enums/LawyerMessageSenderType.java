package api.ailawyer.uz.enums;

/**
 * Advokat chatidagi xabarni kim yuborganini bildiradi.
 * <p>
 * {@link api.ailawyer.uz.entity.LawyerMessageEntity#senderType} maydonida saqlanadi.
 */
public enum LawyerMessageSenderType {

    /** Oddiy foydalanuvchi (mijoz) yuborgan xabar */
    USER,

    /** Advokat yuborgan xabar */
    LAWYER
}
