package api.ailawyer.uz.dto.legal;

import api.ailawyer.uz.enums.DocumentStatus;
import api.ailawyer.uz.enums.DocumentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class LegalDocumentUploadResponseDTO {

    private UUID documentId;
    private DocumentType type;
    private String docNumber;
    private LocalDate docDate;
    private String title;
    private DocumentStatus status;
    private int chunkCount;
}
