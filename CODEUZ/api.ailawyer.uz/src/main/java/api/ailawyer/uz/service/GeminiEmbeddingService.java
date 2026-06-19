package api.ailawyer.uz.service;

import api.ailawyer.uz.exps.GeminiApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini text-embedding-004 orqali matnni 768 o'lchamli vektorga aylantiradi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiEmbeddingService {

    private static final int EXPECTED_DIMENSIONS = 768;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.embedding.model:text-embedding-004}")
    private String embeddingModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new GeminiApiException("Embedding uchun matn bo'sh!");
        }

        String normalizedKey = normalizeProperty(apiKey);
        if (normalizedKey.isBlank() || "YOUR_GEMINI_API_KEY".equals(normalizedKey)) {
            throw new GeminiApiException("Gemini API kaliti sozlanmagan!");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + embeddingModel + ":embedContent?key=" + normalizedKey;

            Map<String, Object> body = new HashMap<>();
            body.put("model", "models/" + embeddingModel);
            body.put("content", Map.of(
                    "parts", List.of(Map.of("text", text))
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GeminiApiException("Gemini embedding API javob bermadi: HTTP "
                        + response.getStatusCode().value());
            }

            return parseEmbeddingResponse(response.getBody());
        } catch (GeminiApiException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Gemini embedding HTTP xatosi: {}", e.getMessage());
            throw new GeminiApiException("Gemini embedding API chaqiruvi muvaffaqiyatsiz: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Gemini embedding javobini parse qilishda xatolik: {}", e.getMessage(), e);
            throw new GeminiApiException("Gemini embedding javobini o'qib bo'lmadi: " + e.getMessage(), e);
        }
    }

    private float[] parseEmbeddingResponse(String responseBody) throws Exception {
        JsonNode values = objectMapper.readTree(responseBody).path("embedding").path("values");
        if (!values.isArray() || values.isEmpty()) {
            throw new GeminiApiException("Gemini embedding javobida 'embedding.values' topilmadi!");
        }

        float[] embedding = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            embedding[i] = (float) values.get(i).asDouble();
        }

        if (embedding.length != EXPECTED_DIMENSIONS) {
            log.warn("Gemini embedding o'lchami kutilganidan farq qiladi: expected={}, actual={}",
                    EXPECTED_DIMENSIONS, embedding.length);
        }

        return embedding;
    }

    private String normalizeProperty(String value) {
        return value == null ? "" : value.trim();
    }
}
