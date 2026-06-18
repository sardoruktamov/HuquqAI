package api.ailawyer.uz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    /** true bo'lsa FCM orqali haqiqiy push yuboriladi */
    private boolean enabled = false;

    /** firebase.credentials.path — nested binding */
    private Credentials credentials = new Credentials();

    /** Firebase service account JSON fayl yo'li */
    public String getCredentialsPath() {
        return credentials != null ? credentials.getPath() : null;
    }

    @Getter
    @Setter
    public static class Credentials {
        private String path = "";
    }
}
