package api.ailawyer.uz.dto.notification;

import api.ailawyer.uz.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceTokenRegisterDTO {

    @NotBlank(message = "FCM token majburiy")
    private String token;

    @NotNull(message = "Platform majburiy")
    private DevicePlatform platform;
}
