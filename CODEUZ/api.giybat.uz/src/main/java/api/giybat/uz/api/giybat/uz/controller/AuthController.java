package api.giybat.uz.api.giybat.uz.controller;

import api.giybat.uz.api.giybat.uz.dto.AppResponse;
import api.giybat.uz.api.giybat.uz.dto.auth.AuthDTO;
import api.giybat.uz.api.giybat.uz.dto.ProfileDTO;
import api.giybat.uz.api.giybat.uz.dto.auth.RegistrationDTO;
import api.giybat.uz.api.giybat.uz.dto.auth.ResetPasswordConfirmDTO;
import api.giybat.uz.api.giybat.uz.dto.auth.ResetPasswordDTO;
import api.giybat.uz.api.giybat.uz.dto.sms.SmsResentDTO;
import api.giybat.uz.api.giybat.uz.dto.sms.SmsVerificationDTO;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "AuthController", description = "API list for Authorization and Authentication")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/registration")
    @Operation(summary = "Profile registration", description = "Api used for registration")
    public ResponseEntity<AppResponse<String>> registration(@Valid @RequestBody RegistrationDTO dto,
                                                            @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        log.info("login: " + dto.getUsername() + " name: " + dto.getName());
        return ResponseEntity.ok().body(authService.registration(dto,lang));
    }

    @GetMapping("/registration/email-verification/{token}")
    @Operation(summary = "Email verification", description = "Api used for Email registration verification")
    public ResponseEntity<String> emailVerification(@PathVariable("token") String token,
                                                  @RequestParam(value = "lang", defaultValue = "UZ") AppLanguage lang){
        log.info("Registration Email verificationtoken: {}", token);
        return ResponseEntity.ok().body(authService.registrationEmailVerification(token,lang));
    }

    @PostMapping("/registration/email-verification-resent")
    @Operation(summary = "Email verification resent", description = "Api used for Email verification resent")
    public ResponseEntity<AppResponse<String>> emailVerificationResent(@Valid @RequestBody SmsResentDTO dto,
                                                                     @RequestParam(value = "lang", defaultValue = "UZ") AppLanguage lang){
        log.info("Registration Email verificationtoken resent: {}", dto);
        return ResponseEntity.ok().body(authService.registrationSmsVerificationResent(dto,lang));
    }

    @PostMapping("/registration/sms-verification")
    @Operation(summary = "SMS verification", description = "Api used for SMS registration verification")
    public ResponseEntity<ProfileDTO> smsVerification(@Valid @RequestBody SmsVerificationDTO dto,
                                                  @RequestParam(value = "lang", defaultValue = "UZ") AppLanguage lang){
        log.info("Registration SMS verificationtoken: {}", dto);
        return ResponseEntity.ok().body(authService.registrationSmsVerification(dto,lang));
    }

    @PostMapping("/registration/sms-verification-resent")
    @Operation(summary = "SMS verification resent", description = "Api used for SMS verification resent")
    public ResponseEntity<AppResponse<String>> smsVerificationResent(@Valid @RequestBody SmsResentDTO dto,
                                                      @RequestParam(value = "lang", defaultValue = "UZ") AppLanguage lang){
        log.info("Registration SMS verificationtoken resent: {}", dto);
        return ResponseEntity.ok().body(authService.registrationSmsVerificationResent(dto,lang));
    }

    @PostMapping("/login")
    @Operation(summary = "login (Auth) API", description = "Api used for login")
    public ResponseEntity<ProfileDTO> login(@Valid @RequestBody AuthDTO dto,
                                            @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        log.info("login: " + dto.getUsername());
        return ResponseEntity.ok().body(authService.login(dto,lang));
    }

    @PostMapping("/registration/reset-password")
    @Operation(summary = "Reset password", description = "Api used for Reset password")
    public ResponseEntity<AppResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordDTO dto,
                                                             @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        log.info("Reset password: {}", dto.getUsername());
        return ResponseEntity.ok().body(authService.resetPassword(dto,lang));
    }

    @PostMapping("/registration/reset-password-confirm")
    @Operation(summary = "Reset password confirm", description = "Api used for Reset password confirm")
    public ResponseEntity<AppResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordConfirmDTO dto,
                                            @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        log.info("Reset password confirm: {}", dto.getUsername());
        return ResponseEntity.ok().body(authService.resetPasswordConfirm(dto,lang));
    }
}