package api.ailawyer.uz.ai;

/**
 * BOSQICH 4 uchun abstraksiya.
 * Keyin Gemini adapter shu interfeysni implement qiladi.
 */
public interface AiProvider {
    AiResponse generate(AiRequest request);
}

