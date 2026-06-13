package api.ailawyer.uz.ai;

import api.ailawyer.uz.exps.AppBadException;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini REST API (v1beta) integratsiyasi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiProviderImpl implements AiProvider {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.model}")
    private String apiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AiResponse generate(AiRequest request) {
        String normalizedKey = normalizeProperty(apiKey);
        if (normalizedKey.isBlank() || "YOUR_GEMINI_API_KEY".equals(normalizedKey)) {
            throw new AppBadException("Gemini API kaliti sozlanmagan!");
        }

        String normalizedModel = normalizeProperty(apiModel);
        if (normalizedModel.isBlank()) {
            throw new AppBadException("Gemini modeli sozlanmagan!");
        }

        long startedAt = System.currentTimeMillis();

        try {
            Map<String, Object> body = buildRequestBody(request);
            String url = buildRequestUrl(normalizedKey, normalizedModel);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AppBadException("Gemini API javob bermadi!");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String text = extractResponseText(root);

            if (text == null || text.isBlank()) {
                throw new AppBadException("AI javob bermadi!");
            }

            AiResponse aiResponse = new AiResponse();
            aiResponse.setText(text.trim());
            aiResponse.setModel(normalizedModel);
            aiResponse.setLatencyMs(System.currentTimeMillis() - startedAt);
            aiResponse.setRagUsed(false);

            JsonNode usage = root.path("usageMetadata");
            if (!usage.isMissingNode()) {
                aiResponse.setTokensIn(usage.path("promptTokenCount").asInt(0));
                aiResponse.setTokensOut(usage.path("candidatesTokenCount").asInt(0));
            }

            return aiResponse;
        } catch (AppBadException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Gemini API chaqiruvi muvaffaqiyatsiz (model={}): {}", normalizedModel, e.getMessage());
            throw new AppBadException("AI javob olishda xatolik yuz berdi!");
        } catch (Exception e) {
            log.error("Gemini javobini qayta ishlashda xatolik: {}", e.getMessage());
            throw new AppBadException("AI javob olishda xatolik yuz berdi!");
        }
    }

    /**
     * application.properties:
     * gemini.api.url = https://generativelanguage.googleapis.com/v1beta/models
     * gemini.api.model = Gemini 2.5 Flash
     * Yakuniy URL: {apiUrl}/{modelId}:generateContent?key={apiKey}
     */
    private String buildRequestUrl(String normalizedKey, String normalizedModel) {
        String baseUrl = normalizeProperty(apiUrl).replaceAll("/$", "");
        String modelId = toApiModelId(normalizedModel);
        return baseUrl + "/" + modelId + ":generateContent?key=" + normalizedKey;
    }

    /**
     * "Gemini 2.5 Flash" -> "gemini-2.5-flash"
     * "gemini-2.5-flash" -> "gemini-2.5-flash"
     */
    private String toApiModelId(String model) {
        if (model.contains(" ")) {
            return model.toLowerCase().replace(' ', '-');
        }
        return model.toLowerCase();
    }

    private String normalizeProperty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("^\"|\"$", "");
    }

    private Map<String, Object> buildRequestBody(AiRequest request) {
        Map<String, Object> body = new HashMap<>();

        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", resolveSystemPrompt(request))));
        body.put("systemInstruction", systemInstruction);

        List<Map<String, Object>> contents = buildContents(request);
        body.put("contents", contents);

        return body;
    }

    private String resolveSystemPrompt(AiRequest request) {
        if (request.getSystemPromptVersion() != null
                && AiPromptVersion.V1.equals(request.getSystemPromptVersion())) {
            return AiSystemPrompt.V1;
        }
        return AiSystemPrompt.V1;
    }

    private List<Map<String, Object>> buildContents(AiRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (AiChatHistoryItem item : request.getHistory()) {
                if (item.getContent() == null || item.getContent().isBlank()) {
                    continue;
                }
                String role = "model".equalsIgnoreCase(item.getRole()) ? "model" : "user";
                contents.add(buildContentPart(role, item.getContent()));
            }
        } else if (request.getPrompt() != null && !request.getPrompt().isBlank()) {
            contents.add(buildContentPart("user", request.getPrompt()));
        } else {
            throw new AppBadException("AI uchun xabar topilmadi!");
        }

        return contents;
    }

    private Map<String, Object> buildContentPart(String role, String text) {
        Map<String, Object> content = new HashMap<>();
        content.put("role", role);
        content.put("parts", List.of(Map.of("text", text)));
        return content;
    }

    private String extractResponseText(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText(null);
            if (text != null && !text.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }
}
