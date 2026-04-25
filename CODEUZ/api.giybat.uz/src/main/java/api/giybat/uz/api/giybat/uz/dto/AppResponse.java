package api.giybat.uz.api.giybat.uz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppResponse<T> {

    private T data;
    private String message;

    public AppResponse(String message) {
        this.message = message;
    }

    public AppResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }
}
