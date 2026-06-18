package api.ailawyer.uz.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK ni ishga tushiradi (faqat firebase.enabled=true bo'lsa).
 * ApplicationReadyEvent — barcha @ConfigurationProperties bog'langanidan keyin ishlaydi.
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;
    private final ResourceLoader resourceLoader;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeFirebase() {
        if (!firebaseProperties.isEnabled()) {
            log.info("Firebase FCM o'chirilgan (firebase.enabled=false)");
            return;
        }

        if (firebaseProperties.getCredentialsPath() == null || firebaseProperties.getCredentialsPath().isBlank()) {
            log.warn("firebase.enabled=true, lekin firebase.credentials.path bo'sh — FCM ishlamaydi");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase allaqachon initsializatsiya qilingan");
            return;
        }

        String credentialsPath = firebaseProperties.getCredentialsPath();
        log.info("Firebase initsializatsiyasi boshlanmoqda, credentialsPath={}", credentialsPath);

        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK muvaffaqiyatli ishga tushirildi: {}", credentialsPath);
            }
        } catch (IOException e) {
            log.error("Firebase JSON faylini o'qishda xatolik: {} — {}", credentialsPath, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Firebase initsializatsiyasida xatolik: {}", e.getMessage(), e);
        }
    }
}
