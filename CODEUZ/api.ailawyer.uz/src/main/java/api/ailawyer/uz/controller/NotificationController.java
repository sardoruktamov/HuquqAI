package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.notification.DeviceTokenRegisterDTO;
import api.ailawyer.uz.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "NotificationController", description = "FCM push bildirishnomalar")
@RequiredArgsConstructor
public class NotificationController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/device-token")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "FCM token ro'yxatdan o'tkazish", description = "Login dan keyin mobil qurilma tokenini yuborish")
    public ResponseEntity<ApiResponse<String>> registerDeviceToken(@Valid @RequestBody DeviceTokenRegisterDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(deviceTokenService.register(dto)));
    }

    @DeleteMapping("/device-token")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "FCM token o'chirish", description = "Logout da device tokenni deaktivatsiya qilish")
    public ResponseEntity<ApiResponse<String>> deleteDeviceToken(@RequestParam("token") String token) {
        return ResponseEntity.ok(ApiResponse.success(deviceTokenService.deactivate(token)));
    }
}
