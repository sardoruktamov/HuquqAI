package api.ailawyer.uz.dto;

import api.ailawyer.uz.enums.GeneralStatus;
import api.ailawyer.uz.enums.ProfileRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // DTO ichidagi null boвЂlgan fieldlarni JSON response ga qoвЂshma degani.

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
