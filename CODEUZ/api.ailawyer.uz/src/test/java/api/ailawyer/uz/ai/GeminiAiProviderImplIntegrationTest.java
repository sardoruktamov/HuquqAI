package api.ailawyer.uz.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GeminiAiProviderImplIntegrationTest {

    @Autowired
    private GeminiAiProviderImpl geminiAiProvider;

    @Test
    void generateReturnsTextWhenApiKeyLoadedFromProperties() {
        AssumptionsEnv.geminiApiKeyMustBeFullLength();

        AiRequest request = new AiRequest();
        request.setPrompt("Salom, qisqa javob bering.");

        AiResponse response = geminiAiProvider.generate(request);

        assertNotNull(response);
        assertNotNull(response.getText());
        assertFalse(response.getText().isBlank());
        System.out.println("Gemini javob: " + response.getText().substring(0, Math.min(120, response.getText().length())));
    }
}
