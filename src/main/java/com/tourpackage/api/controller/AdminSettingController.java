package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.UpdateSettingsRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.SettingResponse;
import com.tourpackage.api.security.AuthenticatedAdmin;
import com.tourpackage.api.service.AdminContentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/settings")
@PreAuthorize("isAuthenticated()")
public class AdminSettingController {

    private final AdminContentService adminContentService;

    public AdminSettingController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping
    public ApiResponse<List<SettingResponse>> list() {
        return ApiResponse.of(adminContentService.listSettings());
    }

    /**
     * Settings reach further than content does — they carry contact details,
     * mail configuration and payment keys — so editing them is restricted to
     * SUPER_ADMIN and ADMIN. EDITOR can manage the catalogue but not the
     * system's own configuration.
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<List<SettingResponse>> update(
            @AuthenticationPrincipal AuthenticatedAdmin principal,
            @Valid @RequestBody UpdateSettingsRequest request) {
        return ApiResponse.of("Settings updated",
                adminContentService.updateSettings(request.values(), principal.id()));
    }

}
