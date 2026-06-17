package api.ailawyer.uz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    /** true bo'lsa FCM orqali haqiqiy push yuboriladi */
    private boolean enabled = false;

    /** Firebase service account JSON fayl yo'li */
    private String credentialsPath = "";
}
