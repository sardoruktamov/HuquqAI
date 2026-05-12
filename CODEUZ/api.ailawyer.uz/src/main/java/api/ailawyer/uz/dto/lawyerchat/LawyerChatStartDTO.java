package api.ailawyer.uz.dto.lawyerchat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Client advokatga birinchi message yozganda ishlatiladigan request DTO.
 * lawyerId + message birga keladi.
 */
@Getter
@Setter
public class LawyerChatStartDTO {
    @NotNull(message = "lawyerId majburiy")
    private Integer lawyerId;

    @Valid
    @NotNull(message = "message majburiy")
    private LawyerMessageCreateDTO message;
}

