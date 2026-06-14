package api.ailawyer.uz.dto.lawyerchat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Advokat chatga xabar yuborish uchun kirish DTO.
 * <p>
 * POST /api/v1/lawyer-chats/{id}/messages va
 * POST /api/v1/lawyer-chats/start (message ichida) da ishlatiladi.
 */
@Getter
@Setter
public class LawyerMessageCreateDTO {

    /** Xabar matni (bo'sh bo'lmasligi kerak) */
    @NotBlank(message = "Xabar bo'sh bo'lmasligi kerak")
    private String content;

    /** Biriktirilgan fayl id lar ro'yxati (attach/upload dan keyin olinadi, ixtiyoriy) */
    private List<String> attachIds;
}
