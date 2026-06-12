package api.ailawyer.uz.ai;

import lombok.Getter;
import lombok.Setter;

/**
 * Gemini API uchun suhbat tarixidagi bitta xabar.
 * role: "user" yoki "model"
 */
@Getter
@Setter
public class AiChatHistoryItem {
    private String role;
    private String content;
}
