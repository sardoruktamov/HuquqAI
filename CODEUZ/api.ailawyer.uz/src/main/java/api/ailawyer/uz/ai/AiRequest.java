package api.ailawyer.uz.ai;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * AI provider'ga yuboriladigan so'rov.
 * prompt — oxirgi user xabari, history — suhbat konteksti, systemPromptVersion — prompt versiyasi.
 */
@Getter
@Setter
public class AiRequest {
    private String prompt;
    private List<AiChatHistoryItem> history;
    private String systemPromptVersion;
    private Map<String, Object> metadata;
}

