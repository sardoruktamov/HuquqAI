package api.ailawyer.uz.dto;

import api.ailawyer.uz.entity.PostEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FilterResultDTO<T> {

    private List<T> list;
    private Long totalCount;

}
