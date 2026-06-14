package api.ailawyer.uz.dto.lawyer;

import lombok.Getter;
import lombok.Setter;

/**
 * Advokatlar katalogini filter qilish uchun parametrlar.
 * <p>
 * GET /public (query param) yoki POST /public/filter (body) da ishlatiladi.
 */
@Getter
@Setter
public class LawyerFilterDTO {

    /** Ism, ixtisoslik yoki hudud bo'yicha erkin qidiruv */
    private String query;

    /** Hudud bo'yicha filter, masalan: "Toshkent" */
    private String region;

    /** Ixtisoslik bo'yicha filter, masalan: "Fuqarolik" */
    private String specialization;

    /** Minimal tajriba yili, masalan: 5 */
    private Integer minExperience;
}
