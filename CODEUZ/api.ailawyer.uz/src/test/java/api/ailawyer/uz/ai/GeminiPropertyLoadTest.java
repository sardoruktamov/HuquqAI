package api.ailawyer.uz.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GeminiPropertyLoadTest {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.model}")
    private String geminiApiModel;

    @Autowired
    private Environment environment;

    @Test
    void geminiApiKeyIsLoadedFromApplicationProperties() {
        String fromEnv = environment.getProperty("gemini.api.key");
        String masked = mask(fromEnv);

        System.out.println("=== GEMINI RUNTIME PROPERTY DEBUG ===");
        System.out.println("Environment.getProperty(\"gemini.api.key\") length: "
                + (fromEnv == null ? "null" : fromEnv.length()));
        System.out.println("Environment.getProperty(\"gemini.api.key\") masked: " + masked);
        System.out.println("@Value injected geminiApiKey length: "
                + (geminiApiKey == null ? "null" : geminiApiKey.length()));
        System.out.println("@Value injected geminiApiKey masked: " + mask(geminiApiKey));
        System.out.println("Process env GEMINI_API_KEY present: "
                + (System.getenv("GEMINI_API_KEY") != null));
        System.out.println("=====================================");

        assertNotNull(fromEnv, "gemini.api.key Environment dan null bo'lmasligi kerak");
        assertFalse(fromEnv.isBlank(), "gemini.api.key bo'sh bo'lmasligi kerak");
        assertNotNull(geminiApiKey, "@Value orqali inject qilingan kalit null bo'lmasligi kerak");
        assertFalse(geminiApiKey.isBlank(), "@Value orqali inject qilingan kalit bo'sh bo'lmasligi kerak");
        org.junit.jupiter.api.Assertions.assertEquals(53, fromEnv.length(),
                "application.properties dagi to'liq kalit uzunligi 53 bo'lishi kerak");
    }

    @Test
    void geminiApiModelIsLoadedFromApplicationProperties() {
        String fromEnv = environment.getProperty("gemini.api.model");

        System.out.println("gemini.api.model from Environment: " + fromEnv);
        System.out.println("@Value injected geminiApiModel: " + geminiApiModel);

        assertNotNull(fromEnv);
        assertFalse(fromEnv.isBlank());
        org.junit.jupiter.api.Assertions.assertEquals("Gemini 2.5 Flash", fromEnv);
        org.junit.jupiter.api.Assertions.assertEquals(fromEnv, geminiApiModel);
    }

    private static String mask(String value) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= 8) {
            return value.isEmpty() ? "(empty)" : "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
