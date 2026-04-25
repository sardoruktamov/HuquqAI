package api.giybat.uz.api.giybat.uz.dto.post;

import api.giybat.uz.api.giybat.uz.dto.AttachDTO;
import api.giybat.uz.api.giybat.uz.dto.ProfileDTO;
import api.giybat.uz.api.giybat.uz.entity.AttachEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDTO {

    private String id;

    private String title;

    private String content;

    private AttachDTO photo;

    private LocalDateTime createdDate;

    private ProfileDTO profile;

}
