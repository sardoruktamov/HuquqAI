package api.giybat.uz.api.giybat.uz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeConfirmDTO {

    @NotBlank(message = "Code required")
    private String code;
}
