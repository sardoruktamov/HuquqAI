package api.ailawyer.uz.controller;

import api.ailawyer.uz.common.response.ApiResponse;
import api.ailawyer.uz.dto.cases.CaseCreateDTO;
import api.ailawyer.uz.dto.cases.CaseDTO;
import api.ailawyer.uz.service.CaseService;
import api.ailawyer.uz.util.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "CaseController", description = "Huquqiy ishlar (xonalarni) boshqarish")
public class CaseController {

    @Autowired
    private CaseService caseService;

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Yangi Case yaratish", description = "Foydalanuvchi yangi muammo (chat xonasi) ochishi")
    public ResponseEntity<ApiResponse<CaseDTO>> create(@Valid @RequestBody CaseCreateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(caseService.create(dto)));
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "O'zining Caselarini olish", description = "Foydalanuvchi o'zi ochgan barcha chat xonalar ro'yxatini olishi")
    public ResponseEntity<ApiResponse<PageImpl<CaseDTO>>> getMyCases(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(caseService.getMyCases(PageUtil.page(page), size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Case haqida to'liq ma'lumot", description = "Bitta case id orqali uning detallarini olish")
    public ResponseEntity<ApiResponse<CaseDTO>> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(caseService.getById(id)));
    }

    @PutMapping("/close/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_LAWYER', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
    @Operation(summary = "Caseni yopish", description = "Muammo hal qilingach, chat xonasini yopish")
    public ResponseEntity<ApiResponse<String>> closeCase(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(caseService.closeCase(id)));
    }
}