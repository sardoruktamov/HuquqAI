package api.ailawyer.uz.dto.lawyer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin advokat arizasini rad etganda yuboriladigan sabab.
 * <p>
 * PUT /api/v1/admin/lawyers/{id}/reject endpointiga body sifatida keladi.
 */
@Getter
@Setter
public class LawyerRejectDTO {

    /** Rad etish sababi — advokat /me da ko'radi va qayta tuzatishi mumkin */
    @NotBlank(message = "Rad etish sababi majburiy")
    private String reason;
}
