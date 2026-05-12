package api.ailawyer.uz.dto.aichat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Yangi AI chat yaratish uchun request DTO.
 */
@Getter
@Setter
public class AiChatCreateDTO {
    @NotBlank(message = "Sarlavha bo'sh bo'lmasligi kerak")
    private String title;
}

