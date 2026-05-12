package api.ailawyer.uz.service.mapper;

import api.ailawyer.uz.enums.GeneralStatus;
import java.time.LocalDateTime;

public interface ProfileDetailMapper {
    Integer getId();
    String getFullName();
    String getUsername();
    String getPhotoId();
    GeneralStatus getStatus();
    LocalDateTime getCreatedDate();
    Long getPostCount();    // advokat postlari soni
    Long getCaseCount();    // user yuridik ishlari soni
    String getRoles();
}
