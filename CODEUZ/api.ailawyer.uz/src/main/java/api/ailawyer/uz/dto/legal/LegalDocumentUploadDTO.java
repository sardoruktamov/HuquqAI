package api.ailawyer.uz.dto.legal;

import api.ailawyer.uz.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class LegalDocumentUploadDTO {

    @NotNull(message = "Hujjat turi majburiy")
    private DocumentType type;

    @NotBlank(message = "Hujjat raqami majburiy")
    private String docNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate docDate;

    @NotBlank(message = "Hujjat nomi majburiy")
    private String title;

    private UUID supersededById;
}
