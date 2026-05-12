package api.ailawyer.uz.dto.aichat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * AI chatga user message yuborish uchun request DTO.
 */
@Getter
@Setter
public class AiMessageCreateDTO {
    @NotBlank(message = "Xabar bo'sh bo'lmasligi kerak")
    private String content;

    private List<String> attachIds;
}

