package api.ailawyer.uz.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FirebasePropertyLoadTest {

    private static final String EXPECTED_CREDENTIALS_PATH =
            "classpath:qalqonai-firebase-adminsdk-fbsvc-cfbe8ceb2e.json";

    @Autowired
    private FirebaseProperties firebaseProperties;

    @Autowired
    private Environment environment;

    @Test
    void firebaseCredentialsPathIsLoadedFromNestedProperty() {
        String fromEnv = environment.getProperty("firebase.credentials.path");

        assertNotNull(fromEnv, "firebase.credentials.path Environment dan null bo'lmasligi kerak");
        assertEquals(EXPECTED_CREDENTIALS_PATH, fromEnv);

        assertNotNull(firebaseProperties.getCredentialsPath(),
                "FirebaseProperties.getCredentialsPath() null bo'lmasligi kerak");
        assertFalse(firebaseProperties.getCredentialsPath().isBlank(),
                "FirebaseProperties.getCredentialsPath() bo'sh bo'lmasligi kerak");
        assertEquals(EXPECTED_CREDENTIALS_PATH, firebaseProperties.getCredentialsPath());
    }

    @Test
    void firebaseAppIsInitializedAfterStartup() {
        assertFalse(FirebaseApp.getApps().isEmpty(),
                "ApplicationReadyEvent dan keyin FirebaseApp initsializatsiya qilingan bo'lishi kerak");
    }
}
