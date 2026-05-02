package api.ailawyer.uz.dto.cases;

import api.ailawyer.uz.enums.CaseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseDTO {
    private String id;
    private String title;
    private CaseStatus status;
    private LocalDateTime createdDate;
    private Integer lawyerId; // Keyinchalik front-end'da advokat ulanganini bilish uchun
}