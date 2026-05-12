package api.ailawyer.uz.dto.lawyerchat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Advokat chatga message yuborish uchun request DTO.
 * Client birinchi yozganda chat avtomatik yaratiladi.
 */
@Getter
@Setter
public class LawyerMessageCreateDTO {
    @NotBlank(message = "Xabar bo'sh bo'lmasligi kerak")
    private String content;

    private List<String> attachIds;
}

