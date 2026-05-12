package api.ailawyer.uz.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Vaqtinchalik AI provider.
 * Gemini integratsiya keyin qo'shiladi; hozir compile va oqim ishlashi uchun stub javob qaytaradi.
 */
@Service
@Primary
public class StubAiProvider implements AiProvider {
    @Override
    public AiResponse generate(AiRequest request) {
        AiResponse res = new AiResponse();
        res.setText("Hozircha AI ulangan emas. Savolingiz qabul qilindi va tez orada javob beriladi.");
        res.setModel("stub");
        res.setTokensIn(null);
        res.setTokensOut(null);
        res.setLatencyMs(0L);
        res.setRagUsed(false);
        return res;
    }
}

