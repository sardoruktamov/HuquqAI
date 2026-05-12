package api.ailawyer.uz.ai;

import lombok.Getter;
import lombok.Setter;

/**
 * AI provider javobi.
 * Bu metadatalar BOSQICH 4–7 (Gemini/RAG)da audit va monitoring uchun kerak bo'ladi.
 */
@Getter
@Setter
public class AiResponse {
    private String text;
    private String model;
    private Integer tokensIn;
    private Integer tokensOut;
    private Long latencyMs;
    private boolean ragUsed;
}

