package api.giybat.uz.api.giybat.uz.dto.post;

import api.giybat.uz.api.giybat.uz.dto.AttachCreateDTO;
import api.giybat.uz.api.giybat.uz.dto.AttachDTO;
import api.giybat.uz.api.giybat.uz.entity.AttachEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostCreateDTO {

    @NotBlank(message = "Title required")
    @Length(min = 5, max = 255, message = "min-5, max-255")
    private String title;

    @NotBlank(message = "Content required")
    private String content;

    @NotNull(message = "Photo required")
    private AttachCreateDTO photo;

}
