package api.ailawyer.uz.dto.post;

import api.ailawyer.uz.dto.AttachDTO;
import api.ailawyer.uz.dto.ProfileDTO;
import api.ailawyer.uz.entity.AttachEntity;
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
