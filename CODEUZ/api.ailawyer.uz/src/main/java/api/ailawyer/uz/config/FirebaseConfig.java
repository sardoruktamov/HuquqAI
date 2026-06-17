package api.ailawyer.uz.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Admin SDK ni ishga tushiradi (faqat firebase.enabled=true bo'lsa).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    @PostConstruct
    public void initializeFirebase() throws IOException {
        if (!firebaseProperties.isEnabled()) {
            log.info("Firebase FCM o'chirilgan (firebase.enabled=false)");
            return;
        }

        if (firebaseProperties.getCredentialsPath() == null || firebaseProperties.getCredentialsPath().isBlank()) {
            log.warn("firebase.enabled=true, lekin firebase.credentials.path bo'sh — FCM ishlamaydi");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(firebaseProperties.getCredentialsPath())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK muvaffaqiyatli ishga tushirildi");
        }
    }
}
