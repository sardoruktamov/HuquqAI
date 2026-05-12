package api.ailawyer.uz.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationDTO {
    @NotBlank(message = "To'liq ism (fullName) kiritish majburiy")
    private String fullName;
    @NotBlank(message = "username required")
    private String username;
    @NotBlank(message = "password required")
    private String password;

}
