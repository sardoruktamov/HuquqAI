package api.giybat.uz.api.giybat.uz.dto.auth;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthDTO {

    private String username;
    private String password;
}
