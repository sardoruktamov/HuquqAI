package api.ailawyer.uz.service.mapper;

import api.ailawyer.uz.enums.GeneralStatus;

import java.time.LocalDateTime;

public interface ProfileDetailMapper {

    Integer getId();

    String getName();

    String getUsername();

    String getPhotoId();

    GeneralStatus getStatus();

    LocalDateTime getCreatedDate();

    Long getPostCount();

    String getRoles();

}
