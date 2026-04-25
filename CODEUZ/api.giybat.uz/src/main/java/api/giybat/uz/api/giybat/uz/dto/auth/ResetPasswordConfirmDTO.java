package api.giybat.uz.api.giybat.uz.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordConfirmDTO {

    @NotBlank(message = "Username required!")
    private String username;    // phone or email

    @NotBlank(message = "Confirm Code required!")
    private String confirmCode;

    @NotBlank(message = "Password required!")
    private String password;
}
