package api.ailawyer.uz.dto.lawyerchat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Mijoz advokatga birinchi xabar yozganda yuboriladigan kirish DTO.
 * <p>
 * POST /api/v1/lawyer-chats/start endpointiga body sifatida keladi.
 * lawyerId va message birga yuboriladi — chat avtomatik yaratiladi.
 */
@Getter
@Setter
public class LawyerChatStartDTO {

    /** Suhbat boshlanadigan advokat profile id si (APPROVED bo'lishi shart) */
    @NotNull(message = "lawyerId majburiy")
    private Integer lawyerId;

    /** Birinchi xabar matni va ixtiyoriy fayllar */
    @Valid
    @NotNull(message = "message majburiy")
    private LawyerMessageCreateDTO message;
}
