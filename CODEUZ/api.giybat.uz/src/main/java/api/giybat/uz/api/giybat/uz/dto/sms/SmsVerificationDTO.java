package api.giybat.uz.api.giybat.uz.dto.sms;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsVerificationDTO {
    @NotBlank(message = "Phone number required")
    private String phoneNumber;
    @NotBlank(message = "Sms code required")
    private String code;

    @Override
    public String toString() {
        return "SmsVerificationDTO{" +
                "phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
