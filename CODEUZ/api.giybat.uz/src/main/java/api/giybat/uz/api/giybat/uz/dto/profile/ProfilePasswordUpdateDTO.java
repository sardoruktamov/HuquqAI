package api.giybat.uz.api.giybat.uz.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfilePasswordUpdateDTO {

    @NotBlank(message = "Current Password required")
    private String currentPswd;

    @NotBlank(message = "New Password required")
    private String newPswd;
}
