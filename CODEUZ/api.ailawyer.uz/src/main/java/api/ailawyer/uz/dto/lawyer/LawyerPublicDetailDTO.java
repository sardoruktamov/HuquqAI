package api.ailawyer.uz.dto.lawyer;

import api.ailawyer.uz.dto.AttachDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Bitta advokatning to'liq public profili.
 * <p>
 * GET /api/v1/lawyers/public/{id} endpointida qaytariladi.
 * {@link LawyerPublicDTO} dan meros — qo'shimcha bio va litsenziya ma'lumotlari.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LawyerPublicDetailDTO extends LawyerPublicDTO {

    /** Advokat haqida batafsil tavsif */
    private String bio;

    /** Advokatlik litsenziyasi raqami */
    private String licenseNumber;

    /** Litsenziya hujjati fayli (URL bilan) */
    private AttachDTO licenseDocument;
}
