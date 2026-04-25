package api.giybat.uz.api.giybat.uz.service;

import api.giybat.uz.api.giybat.uz.dto.AppResponse;
import api.giybat.uz.api.giybat.uz.dto.auth.AuthDTO;
import api.giybat.uz.api.giybat.uz.dto.ProfileDTO;
import api.giybat.uz.api.giybat.uz.dto.auth.RegistrationDTO;
import api.giybat.uz.api.giybat.uz.dto.auth.ResetPasswordConfirmDTO;
import api.giybat.uz.api.giybat.uz.dto.auth.ResetPasswordDTO;
import api.giybat.uz.api.giybat.uz.dto.sms.SmsResentDTO;
import api.giybat.uz.api.giybat.uz.dto.sms.SmsVerificationDTO;
import api.giybat.uz.api.giybat.uz.entity.ProfileEntity;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.enums.GeneralStatus;
import api.giybat.uz.api.giybat.uz.enums.ProfileRole;
import api.giybat.uz.api.giybat.uz.exps.AppBadException;
import api.giybat.uz.api.giybat.uz.repository.ProfileRepository;
import api.giybat.uz.api.giybat.uz.repository.ProfileRoleRepository;
import api.giybat.uz.api.giybat.uz.util.EmailUtil;
import api.giybat.uz.api.giybat.uz.util.JwtUtil;
import api.giybat.uz.api.giybat.uz.util.PhoneUtil;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private ProfileRoleRepository profileRoleRepository;
    @Autowired
    private ProfileRoleService profileRoleService;
    @Autowired
    private EmailSendingService emailSendingService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private ResourceBundleService bundleService;
    @Autowired
    private SmsSendService smsSendService;
    @Autowired
    private SmsHistoryService smsHistoryService;
    @Autowired EmailHistoryService emailHistoryService;
    @Autowired
    private AttachService attachService;

    public AppResponse<String> registration(RegistrationDTO dto, AppLanguage lang){

        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isPresent()){
            ProfileEntity profile = optional.get();
            if (profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)){
                profileRoleService.deleteRoles(profile.getId());
                // 1-usul
                 profileRepository.delete(profile);
                // 2-usul
                //send sms/email orqali ro'yxatdan o'tishini davom ettirish
            }else {
                log.warn("Profile already exists with name {}", dto.getUsername());
                throw new AppBadException(bundleService.getMessage("email.phone.exist", lang));
            }
        }

        ProfileEntity entity = new ProfileEntity();
        entity.setName(dto.getName());
        entity.setUsername(dto.getUsername());
        entity.setPassword(bCryptPasswordEncoder.encode(dto.getPassword()));
        entity.setStatus(GeneralStatus.IN_REGISTRATION);
        entity.setVisible(true);
        entity.setCreatedDate(LocalDateTime.now());
        profileRepository.save(entity);
        // Insert Role
        profileRoleService.create(entity.getId(), ProfileRole.ROLE_USER);

        // Usernameni tekshirish email yoki phone ekanligiga >>>PASTDA 2-USUL<<<<<
        if (EmailUtil.isEmail(dto.getUsername())){
            // send email
            emailSendingService.sendEmailForRegistration(dto.getUsername(), entity.getId(), lang);
        }else if (PhoneUtil.isPhone(dto.getUsername())){
            // send SMS
            smsSendService.sendRegistrationSms(dto.getUsername(), lang);
        }

        return new AppResponse<>(bundleService.getMessage("email.confirm.send",lang));
    }

    public String registrationEmailVerification(String token, AppLanguage lang) {

        try{
            Integer profileId = JwtUtil.decodeRegVerToken(token);
            ProfileEntity profile = profileService.getById(profileId, lang);
            if (profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)){
                // 1-usulda barcha fieldlarini update qiladi
//            profile.setStatus(GeneralStatus.ACTIVE);
//            profileRepository.save(profile);
                // 2-usulda faqat status update bo`ladi
                profileRepository.changeStatus(profileId,GeneralStatus.ACTIVE);
                return bundleService.getMessage("verification.finished",lang);
            }
        }catch (JwtException e){
        }
        log.warn("Registration email verification failed {}", token);
        throw new AppBadException(bundleService.getMessage("verification.failed",lang));
    }

    public ProfileDTO login(AuthDTO dto, AppLanguage lang){
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isEmpty()){
            log.warn("Username or Password wrong {}", dto.getUsername());
            throw new AppBadException(bundleService.getMessage("username.password.wrong",lang));
        }
        ProfileEntity profile = optional.get();
        if(!bCryptPasswordEncoder.matches(dto.getPassword(), profile.getPassword())){
            throw new AppBadException(bundleService.getMessage("username.password.wrong",lang));
        }
        if (!profile.getStatus().equals(GeneralStatus.ACTIVE)){
            log.warn("Wrong status: {}", dto.getUsername());
            throw new AppBadException(bundleService.getMessage("status.error.register.again",lang));
        }

        // response
        return getLoginResponse(profile);
    }

    public ProfileDTO registrationSmsVerification(SmsVerificationDTO dto, AppLanguage lang) {
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getPhoneNumber());
        if (optional.isEmpty()){
            log.warn("Verification failed: {}", dto.getPhoneNumber());
            throw new AppBadException(bundleService.getMessage("profile.not.found",lang));
        }
        ProfileEntity profile = optional.get();
        // checking status
        if (!profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)){
            log.warn("Wrong status: {}", dto.getPhoneNumber());
            throw new AppBadException(bundleService.getMessage("verification.failed",lang));
        }
        // checking sms code
        smsHistoryService.check(dto.getPhoneNumber(), dto.getCode(), lang);
        // ACTIVE
        profileRepository.changeStatus(profile.getId(),GeneralStatus.ACTIVE);
        // response
        return getLoginResponse(profile);
    }

    public AppResponse<String> registrationSmsVerificationResent(SmsResentDTO dto, AppLanguage lang) {
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getPhoneNumber());

        if (optional.isEmpty()){
            log.warn("Verification failed: {}", dto.getPhoneNumber());
            throw new AppBadException(bundleService.getMessage("profile.not.found",lang));
        }
        ProfileEntity profile = optional.get();
        // checking status
        if (!profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)){
            log.warn("Wrong status: {}", dto.getPhoneNumber());
            throw new AppBadException(bundleService.getMessage("verification.failed",lang));
        }
        smsSendService.sendRegistrationSms(dto.getPhoneNumber(), lang);
        return new AppResponse<>(bundleService.getMessage("sms.resend",lang));
    }

    public AppResponse<String> resetPassword(ResetPasswordDTO dto, AppLanguage lang) {

        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isEmpty()){
            log.warn("Profile not  found: {}", dto.getUsername());
            throw new AppBadException(bundleService.getMessage("profile.not.found",lang));
        }
        ProfileEntity profile = optional.get();

        if (!profile.getStatus().equals(GeneralStatus.ACTIVE)){
            log.warn("Wrong status: {}", dto.getUsername());
            throw new AppBadException(bundleService.getMessage("status.error.register.again",lang));
        }

        //  sms or email send
        if (EmailUtil.isEmail(dto.getUsername())){
            // send email
            emailSendingService.sendResetPasswordEmail(dto.getUsername(), lang);
        }else if (PhoneUtil.isPhone(dto.getUsername())){
            // send SMS
            smsSendService.sendResetPasswordSms(dto.getUsername(), lang);
        }
        String responseMessage = bundleService.getMessage("resent.password.code.sent",lang);
        return new AppResponse<String>(String.format(responseMessage,dto.getUsername()));
    }

    public AppResponse<String> resetPasswordConfirm(ResetPasswordConfirmDTO dto, AppLanguage lang) {
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isEmpty()){
            log.warn("Profile not  found: {}", dto.getUsername());
            throw new AppBadException(bundleService.getMessage("profile.not.found",lang));
        }
        ProfileEntity profile = optional.get();
        // checking status
        if (!profile.getStatus().equals(GeneralStatus.ACTIVE)){
            log.warn("Wrong status: {}", dto.getUsername());
            throw new AppBadException(bundleService.getMessage("status.error.register.again",lang));
        }
    // Usernameni tekshirish email yoki phone ekanligiga
        if (EmailUtil.isEmail(dto.getUsername())){
            // send email
            emailHistoryService.check(dto.getUsername(), dto.getConfirmCode(), lang);
        }else if (PhoneUtil.isPhone(dto.getUsername())){
            // send SMS
            smsHistoryService.check(dto.getUsername(), dto.getConfirmCode(), lang);
        }
        // update password
        profileRepository.updatePassword(profile.getId(), bCryptPasswordEncoder.encode(dto.getPassword()));
        // return
        return new AppResponse<String>(bundleService.getMessage("password.changed.successfully",lang));
    }

    public ProfileDTO getLoginResponse(ProfileEntity profile){
        ProfileDTO response = new ProfileDTO();
        response.setName(profile.getName());
        response.setUsername(profile.getUsername());
        response.setRoleList(profileRoleRepository.getAllRolesListByProfileId(profile.getId()));
        response.setJwt(JwtUtil.encode(profile.getUsername(), profile.getId(),response.getRoleList()));        // jwt
        response.setPhoto(attachService.attachDTO(profile.getPhotoId()));
        return response;
    }


}

/*
>>> 2-USUL Usernameni tekshirish email yoki phone ekanligiga<<<<<
@Service
@Slf4j
public class AuthService {

    public AppResponse<String> registration(RegistrationDTO dto, AppLanguage lang) {
        String username = dto.getUsername();
        String type = checkEmailOrPhone(username);

        if ("Email".equals(type)) {
            // Email keldi
            log.info("Email detected: {}", username);
            emailSendingService.sendEmailForRegistration(username, entity.getId(), lang);
        } else if ("Phone".equals(type)) {
            // Telefon raqam keldi
            log.info("Phone number detected: {}", username);
            smsSendService.sendRegistrationSms(username);
        } else {
            throw new IllegalArgumentException("Invalid email or phone number: " + username);
        }

        return AppResponse.<String>builder()
                .message("Registration successful")
                .data("Success")
                .build();
    }

    private String checkEmailOrPhone(String value) {
        // Regular expression for validating email
        String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        // Regular expression for validating phone numbers
        String phoneRegex = "^998\\d{9}$"; // Uzbekistan phone format

        if (value.matches(emailRegex)) {
            return "Email";
        } else if (value.matches(phoneRegex)) {
            return "Phone";
        } else {
            return "Invalid";
        }
    }
}*/
