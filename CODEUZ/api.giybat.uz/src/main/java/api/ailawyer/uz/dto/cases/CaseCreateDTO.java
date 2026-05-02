package api.ailawyer.uz.dto.cases;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseCreateDTO {
    @NotBlank(message = "Title (mavzu) kiritish majburiy!")
    private String title;
}