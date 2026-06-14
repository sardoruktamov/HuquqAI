package api.ailawyer.uz.dto.lawyer;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Tasdiqlangan advokat o'z profilini qisman yangilash uchun DTO.
 * <p>
 * PUT /api/v1/lawyers/profile endpointiga yuboriladi.
 * Faqat yuborilgan (null emas) maydonlar yangilanadi.
 */
@Getter
@Setter
public class LawyerProfileUpdateDTO {

    /** Yangi ixtisosliklar (vergul bilan) */
    private String specializations;

    /** Yangi tajriba yili */
    private Integer experienceYears;

    /** Yangi bio matni */
    private String bio;

    /** Yangi litsenziya raqami */
    private String licenseNumber;

    /** Yangi litsenziya hujjati attach id si */
    private String licenseDocumentId;

    /** Yangi hudud */
    private String region;

    /** Yangi tillar ro'yxati */
    private List<String> languages;

    /** Yangi mijoz qabul qiladimi (true/false) */
    private Boolean isAvailable;
}
