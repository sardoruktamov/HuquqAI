package api.ailawyer.uz.service;

import api.ailawyer.uz.config.CustomUserDetails;
import api.ailawyer.uz.entity.ProfileEntity;
import api.ailawyer.uz.enums.GeneralStatus;
import api.ailawyer.uz.enums.ProfileRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

final class TestSecurityHelper {

    private TestSecurityHelper() {
    }

    static void loginAs(Integer userId, ProfileRole... roles) {
        ProfileEntity profile = new ProfileEntity();
        profile.setId(userId);
        profile.setFullName("Test User");
        profile.setUsername("test@mail.ru");
        profile.setPassword("secret");
        profile.setStatus(GeneralStatus.ACTIVE);

        CustomUserDetails user = new CustomUserDetails(profile, List.of(roles));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    static void clear() {
        SecurityContextHolder.clearContext();
    }
}
