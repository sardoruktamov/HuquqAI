package api.ailawyer.uz.ai;

import org.junit.jupiter.api.Assumptions;

final class AssumptionsEnv {

    private AssumptionsEnv() {
    }

    static void geminiApiKeyMustBeFullLength() {
        String key = System.getenv("GEMINI_API_KEY");
        Assumptions.assumeTrue(key == null || key.length() >= 20,
                "GEMINI_API_KEY env o'zgaruvchisi placeholder '...' bilan override qilgan — test o'tkazib yuborildi");
    }
}
