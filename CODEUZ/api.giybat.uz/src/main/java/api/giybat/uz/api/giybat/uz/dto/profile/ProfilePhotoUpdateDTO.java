package api.giybat.uz.api.giybat.uz.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfilePhotoUpdateDTO {

    @NotBlank(message = "Photo required")
    private String photoId;
}
