package api.ailawyer.uz.dto;

import api.ailawyer.uz.enums.ProfileRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JwtDTO {

    private String username;
    private Integer id;
    private List<ProfileRole> roleList;
}
