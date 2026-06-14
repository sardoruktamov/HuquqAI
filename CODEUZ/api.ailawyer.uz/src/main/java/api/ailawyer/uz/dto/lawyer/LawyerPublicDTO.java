package api.ailawyer.uz.dto.lawyer;

import api.ailawyer.uz.dto.AttachDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tasdiqlangan advokatning public katalog uchun qisqa ma'lumotlari.
 * <p>
 * GET /api/v1/lawyers/public va filter endpointlarida qaytariladi.
 * Litsenziya raqami va hujjat bu DTO da yo'q — faqat detail da.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LawyerPublicDTO {

    /** Advokat profile id si */
    private Integer id;

    /** Advokat to'liq ismi */
    private String fullName;

    /** Profil rasmi (URL bilan) */
    private AttachDTO photo;

    /** Ixtisosliklar ro'yxati */
    private List<String> specializations;

    /** Tajriba yili */
    private Integer experienceYears;

    /** Hudud/shahar */
    private String region;

    /** Xizmat ko'rsatadigan tillar */
    private List<String> languages;

    /** Hozir yangi mijoz qabul qiladimi */
    private Boolean isAvailable;

    /** Advokatning platformadagi postlar soni */
    private Long postCount;

    /** Admin tasdiqlagan vaqt */
    private LocalDateTime verifiedAt;
}
