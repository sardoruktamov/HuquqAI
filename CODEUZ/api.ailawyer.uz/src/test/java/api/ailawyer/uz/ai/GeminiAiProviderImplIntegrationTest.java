package api.ailawyer.uz.ai;

import api.ailawyer.uz.entity.LawChunkEntity;
import api.ailawyer.uz.service.LegalSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class GeminiAiProviderImplIntegrationTest {

    @Autowired
    private GeminiAiProviderImpl geminiAiProvider;

    @MockBean
    private LegalSearchService legalSearchService;

    @Test
    void generateReturnsTextWhenApiKeyLoadedFromProperties() {
        AssumptionsEnv.geminiApiKeyMustBeFullLength();
        when(legalSearchService.searchRelevantContext(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        AiRequest request = new AiRequest();
        request.setPrompt("Salom, qisqa javob bering.");

        AiResponse response = geminiAiProvider.generate(request);

        assertNotNull(response);
        assertNotNull(response.getText());
        assertFalse(response.getText().isBlank());
        System.out.println("Gemini javob: " + response.getText().substring(0, Math.min(120, response.getText().length())));
    }
}
