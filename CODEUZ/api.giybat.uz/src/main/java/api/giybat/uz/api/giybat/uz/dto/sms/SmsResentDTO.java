package api.giybat.uz.api.giybat.uz.dto.sms;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsResentDTO {
    @NotBlank(message = "Phone number required")
    private String phoneNumber;

    @Override
    public String toString() {
        return "SmsResentDTO{" +
                "phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
