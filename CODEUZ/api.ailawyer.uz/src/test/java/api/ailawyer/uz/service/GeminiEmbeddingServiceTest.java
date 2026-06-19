package api.ailawyer.uz.service;

import api.ailawyer.uz.exps.GeminiApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiEmbeddingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiEmbeddingService geminiEmbeddingService;

    @BeforeEach
    void setUp() {
        geminiEmbeddingService = new GeminiEmbeddingService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(geminiEmbeddingService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiEmbeddingService, "embeddingModel", "text-embedding-004");
    }

    @Test
    void getEmbedding_parses768DimensionalVector() {
        String responseBody = """
                {
                  "embedding": {
                    "values": [0.1, 0.2, 0.3]
                  }
                }
                """;

        when(restTemplate.exchange(
                eq("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=test-api-key"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<?>>any(),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        float[] embedding = geminiEmbeddingService.getEmbedding("test matn");

        assertEquals(3, embedding.length);
        assertEquals(0.1f, embedding[0], 0.0001f);
        assertEquals(0.2f, embedding[1], 0.0001f);
        assertEquals(0.3f, embedding[2], 0.0001f);
    }

    @Test
    void getEmbedding_throwsWhenTextBlank() {
        assertThrows(GeminiApiException.class, () -> geminiEmbeddingService.getEmbedding("  "));
    }
}
