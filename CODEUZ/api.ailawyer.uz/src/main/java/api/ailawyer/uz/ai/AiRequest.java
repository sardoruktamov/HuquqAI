package api.ailawyer.uz.ai;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * AI provider'ga yuboriladigan so'rov.
 * prompt — user savoli + kontekst, systemPromptVersion — prompt'ni versiyalash uchun.
 */
@Getter
@Setter
public class AiRequest {
    private String prompt;
    private String systemPromptVersion;
    private Map<String, Object> metadata;
}

