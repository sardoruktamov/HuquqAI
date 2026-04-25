package api.giybat.uz.api.giybat.uz.dto;

import api.giybat.uz.api.giybat.uz.enums.GeneralStatus;
import api.giybat.uz.api.giybat.uz.enums.ProfileRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // DTO ichidagi null bo‘lgan fieldlarni JSON response ga qo‘shma degani.

public class ProfileDTO {

    private Integer id;
    private String name;
    private String username;
    private List<ProfileRole> roleList;
    private String jwt;
    private LocalDateTime createdDate;
    private AttachDTO photo;
    private GeneralStatus status;
    private Long postCount;
}
