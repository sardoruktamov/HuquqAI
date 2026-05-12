package api.ailawyer.uz.service.mapper;

import api.ailawyer.uz.enums.GeneralStatus;

import java.time.LocalDateTime;

public interface PostDetailMapper {

    String getPostId();
    String getPostTitle();
    String getPostPhotoId();
    LocalDateTime getPostCreatedDate();
    Integer getProfileId();
    String getProfileName();
    String getProfileUsername();


}
