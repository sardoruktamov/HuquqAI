package api.ailawyer.uz.dto.lawyer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Advokat onboarding (birinchi ro'yxatdan o'tish) uchun kirish ma'lumotlari.
 * <p>
 * POST /api/v1/lawyers/onboarding endpointiga yuboriladi.
 */
@Getter
@Setter
public class LawyerOnboardingDTO {

    /** Ixtisosliklar — vergul bilan yoki bitta matn, masalan: "Fuqarolik huquqi,Meros" */
    @NotBlank(message = "Ixtisoslik majburiy")
    private String specializations;

    /** Ish tajribasi yil hisobida */
    @NotNull(message = "Tajriba yili majburiy")
    private Integer experienceYears;

    /** Advokat haqida qisqa tavsif (ixtiyoriy) */
    private String bio;

    /** Advokatlik litsenziyasi raqami */
    @NotBlank(message = "Litsenziya raqami majburiy")
    private String licenseNumber;

    /** Yuklangan litsenziya hujjati fayl id si (attach upload dan keyin) */
    @NotBlank(message = "Litsenziya hujjati majburiy")
    private String licenseDocumentId;

    /** Faoliyat yuritadigan hudud/shahar */
    @NotBlank(message = "Hudud majburiy")
    private String region;

    /** Xizmat ko'rsatadigan tillar ro'yxati, masalan: ["UZ", "RU"] */
    private List<String> languages;
}
