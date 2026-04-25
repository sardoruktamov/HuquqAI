package api.giybat.uz.api.giybat.uz.dto.sms;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsSendResponseDTO {

    private String id;
    private String message;
    private String status;
}
