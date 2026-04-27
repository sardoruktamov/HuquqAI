package api.ailawyer.uz.controller;

import api.ailawyer.uz.dto.AppResponse;
import api.ailawyer.uz.dto.CodeConfirmDTO;
import api.ailawyer.uz.dto.ProfileDTO;
import api.ailawyer.uz.dto.profile.*;
import api.ailawyer.uz.enums.AppLanguage;
import api.ailawyer.uz.service.ProfileService;
import api.ailawyer.uz.util.PageUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@Slf4j
@Tag(name = "ProfileController", description = "API list for working with Profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @PutMapping("/detail")
    public ResponseEntity<AppResponse<String>> updateDetail(@Valid @RequestBody ProfileDetailUpdateDTO dto,
                                                      @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        AppResponse<String> res = profileService.updateDetail(dto, lang);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/photo")
    public ResponseEntity<AppResponse<String>> updatePhoto(@Valid @RequestBody ProfilePhotoUpdateDTO dto,
                                                            @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        AppResponse<String> res = profileService.updatePhoto(dto.getPhotoId(), lang);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/password")
    public ResponseEntity<AppResponse<String>> updatePassword(@Valid @RequestBody ProfilePasswordUpdateDTO dto,
                                                            @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        AppResponse<String> res = profileService.updatePassword(dto, lang);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/username")
    public ResponseEntity<AppResponse<String>> updateUsername(@Valid @RequestBody ProfileUsernameUpdateDTO dto,
                                                              @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        AppResponse<String> res = profileService.updateUsername(dto, lang);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/username/confirm")
    public ResponseEntity<AppResponse<String>> updateUsernameConfirm(@Valid @RequestBody CodeConfirmDTO dto,
                                                              @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang){
        AppResponse<String> res = profileService.updateUsernameConfirm(dto, lang);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/filter")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<PageImpl<ProfileDTO>> filter(@RequestBody ProfileFilterDTO dto,
                                                       @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang,
                                                       @RequestParam(value = "page", defaultValue = "1") int page,
                                                       @RequestParam(value = "size", defaultValue = "10") int size
                                                      ){
        return ResponseEntity.ok(profileService.filter(dto, PageUtil.page(page), size, lang));
    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AppResponse<String>> status(@PathVariable("id") Integer id,
                   @RequestBody ProfileSatusDTO dto,
                   @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang
                   ){
        return ResponseEntity.ok(profileService.changeStatus(id, dto.getStatus(), lang));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AppResponse<String>> delete(@PathVariable("id") Integer id,
                                                      @RequestHeader(value = "Accept-Language", defaultValue = "UZ") AppLanguage lang
    ){
        return ResponseEntity.ok(profileService.delete(id, lang));
    }

}
