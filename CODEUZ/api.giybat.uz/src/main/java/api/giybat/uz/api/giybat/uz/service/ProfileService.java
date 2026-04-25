package api.giybat.uz.api.giybat.uz.service;

import api.giybat.uz.api.giybat.uz.dto.AppResponse;
import api.giybat.uz.api.giybat.uz.dto.CodeConfirmDTO;
import api.giybat.uz.api.giybat.uz.dto.ProfileDTO;
import api.giybat.uz.api.giybat.uz.dto.profile.*;
import api.giybat.uz.api.giybat.uz.entity.AttachEntity;
import api.giybat.uz.api.giybat.uz.entity.ProfileEntity;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.enums.GeneralStatus;
import api.giybat.uz.api.giybat.uz.enums.ProfileRole;
import api.giybat.uz.api.giybat.uz.exps.AppBadException;
import api.giybat.uz.api.giybat.uz.repository.ProfileRepository;
import api.giybat.uz.api.giybat.uz.repository.ProfileRoleRepository;
import api.giybat.uz.api.giybat.uz.service.mapper.ProfileDetailMapper;
import api.giybat.uz.api.giybat.uz.util.EmailUtil;
import api.giybat.uz.api.giybat.uz.util.JwtUtil;
import api.giybat.uz.api.giybat.uz.util.PhoneUtil;
import api.giybat.uz.api.giybat.uz.util.SpringSecurityUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    ResourceBundleService bundleService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private SmsSendService smsSendService;
    @Autowired
    private EmailSendingService emailSendingService;
    @Autowired
    private SmsHistoryService smsHistoryService;
    @Autowired
    private EmailHistoryService emailHistoryService;
    @Autowired
    private ProfileRoleRepository profileRoleRepository;
    @Autowired
    private AttachService attachService;

    public AppResponse<String> updateDetail(ProfileDetailUpdateDTO dto, AppLanguage lang){
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        profileRepository.updateDetail(userId, dto.getName());
        return new AppResponse<>(bundleService.getMessage("profile.update.detail.success", lang));
    }

    public AppResponse<String> updatePassword(ProfilePasswordUpdateDTO dto, AppLanguage lang) {
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile = getById(userId, lang);
        if (!bCryptPasswordEncoder.matches(dto.getCurrentPswd(), profile.getPassword())){
            throw new AppBadException(bundleService.getMessage("current.password.incorrectly",lang));
        }

        profileRepository.updatePassword(userId, bCryptPasswordEncoder.encode(dto.getNewPswd()));
        return new AppResponse<String>(bundleService.getMessage("password.changed.successfully",lang));
    }

    public AppResponse<String> updateUsername(ProfileUsernameUpdateDTO dto, AppLanguage lang){
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        // check
        if (optional.isPresent()){
            throw new AppBadException(bundleService.getMessage("email.phone.exist", lang));
        }

        // send sms
        if (EmailUtil.isEmail(dto.getUsername())){
            // send email
            emailSendingService.sendChangeUsernameEmail(dto.getUsername(), lang);
        }else if (PhoneUtil.isPhone(dto.getUsername())){
            // send SMS
            smsSendService.sendUsernameChangeConfirmSms(dto.getUsername(), lang);
        }
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        profileRepository.updateTempUsername(userId, dto.getUsername());
        String response = bundleService.getMessage("resent.password.code.sent", lang);
        return new AppResponse<>(String.format(response,dto.getUsername()));
    }

    public AppResponse<String> updateUsernameConfirm(CodeConfirmDTO dto, AppLanguage lang) {
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile = getById(userId, lang);
        String tempUsername = profile.getTempUsername();
        // check
        if (EmailUtil.isEmail(tempUsername)){
            // send email
            emailHistoryService.check(tempUsername, dto.getCode(), lang);
        }else if (PhoneUtil.isPhone(tempUsername)){
            // send SMS
            smsHistoryService.check(tempUsername, dto.getCode(), lang);
        }
        // update username
        profileRepository.updateUsername(userId,tempUsername);
        // response
        List<ProfileRole> roles = profileRoleRepository.getAllRolesListByProfileId(profile.getId());
        String jwt = JwtUtil.encode(tempUsername,profile.getId(),roles);

        return new AppResponse<>(jwt, bundleService.getMessage("change.username.succes", lang));
    }

    public AppResponse<String> updatePhoto(String photoId, AppLanguage lang) {
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile = getById(userId,lang);
        profileRepository.updatePhoto(userId,photoId);
        if (profile.getPhotoId() != null && !profile.getPhotoId().equals(photoId)){
            attachService.delete(profile.getPhotoId()); // delete old img
        }
        return new AppResponse<>(bundleService.getMessage("change.photo.succes", lang));
    }



    public ProfileEntity getById(int id, AppLanguage lang){
        // 1-usul
//        Optional<ProfileEntity> optional = profileRepository.findByIdAndVisibleTrue(id);
//        if(optional.isEmpty()){
//            log.error("Profile not found: {}",id);
//            throw new AppBadException("Profile not found");
//        }
//        return optional.get();
        // 2-usul
        return profileRepository.findByIdAndVisibleTrue(id).orElseThrow( () -> {
            log.error("Profile not found: {}",id);
            throw new AppBadException(bundleService.getMessage("profile.not.found", lang));
        });
    }


    public PageImpl<ProfileDTO> filter(ProfileFilterDTO dto, int page, int size, AppLanguage lang) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ProfileDetailMapper> filterResult = null;
        if (dto.getQuery() == null){
            filterResult = profileRepository.findAllByVisibleIsTrueOrderByCreatedDateDesc(pageRequest);
        }else {
            filterResult = profileRepository.filter("%" + dto.getQuery().toLowerCase() + "%", pageRequest);
        }
        List<ProfileDTO> resultList =  filterResult.stream().map(this::toDto).toList();
        return new PageImpl<>(resultList, pageRequest,filterResult.getTotalElements());
    }

    public AppResponse<String> changeStatus(Integer id, GeneralStatus status, AppLanguage lang) {
        profileRepository.changeStatus(id, status);
        return new AppResponse<>(bundleService.getMessage("change.status.succes", lang));
    }

    public AppResponse<String> delete(Integer id, AppLanguage lang) {
        profileRepository.deleteVisibleFalse(id);
        return new AppResponse<>(bundleService.getMessage("profile.delete.succes", lang));
    }
    public ProfileDTO toDto(ProfileEntity entity){
        ProfileDTO dto = new ProfileDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUsername(entity.getUsername());
        dto.setStatus(entity.getStatus());
        dto.setCreatedDate(entity.getCreatedDate());
        if (entity.getRoleList() != null){
            List<ProfileRole> rolList =  entity.getRoleList().stream().map(item -> item.getRoles()).toList();
            dto.setRoleList(rolList);
        }
        dto.setPhoto(attachService.attachDTO(entity.getPhotoId()));
        return dto;
    }

    public ProfileDTO toDto(ProfileDetailMapper mapper){
        ProfileDTO dto = new ProfileDTO();
        dto.setId(mapper.getId());
        dto.setName(mapper.getName());
        dto.setUsername(mapper.getUsername());
        dto.setStatus(mapper.getStatus());
        dto.setCreatedDate(mapper.getCreatedDate());
        if (mapper.getRoles() != null){
            List<ProfileRole> roleList = Arrays.stream(mapper.getRoles().split(","))
                    .map(roleName -> ProfileRole.valueOf(roleName))
                    .toList();
            dto.setRoleList(roleList);
        }
        dto.setPhoto(attachService.attachDTO(mapper.getPhotoId()));
        dto.setPostCount(mapper.getPostCount());
        return dto;
    }


}
