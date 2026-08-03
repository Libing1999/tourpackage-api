package com.tourpackage.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.FaqRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.FaqAdminResponse;
import com.tourpackage.api.service.AdminContentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/faqs")
@PreAuthorize("isAuthenticated()")
public class AdminFaqController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final AdminContentService adminContentService;

    public AdminFaqController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping
    public ApiResponse<List<FaqAdminResponse>> list() {
        return ApiResponse.of(adminContentService.listFaqs());
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FaqAdminResponse> create(@Valid @RequestBody FaqRequest request) {
        return ApiResponse.of("FAQ created", adminContentService.createFaq(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<FaqAdminResponse> update(@PathVariable UUID id, @Valid @RequestBody FaqRequest request) {
        return ApiResponse.of("FAQ updated", adminContentService.updateFaq(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        adminContentService.deleteFaq(id);
        return ApiResponse.message("FAQ deleted");
    }

}
