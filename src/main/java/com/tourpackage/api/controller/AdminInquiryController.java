package com.tourpackage.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.UpdateInquiryStatusRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.InquiryAdminListResponse;
import com.tourpackage.api.dto.response.InquiryResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.InquiryStatus;
import com.tourpackage.api.service.InquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/inquiries")
@PreAuthorize("isAuthenticated()")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    public AdminInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<InquiryAdminListResponse>> list(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.of(inquiryService.list(status, search, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<InquiryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(inquiryService.getById(id));
    }

    /** Working the enquiry inbox is support work, so SUPPORT is included
     * alongside the content roles — same reasoning as booking status. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SUPPORT')")
    public ApiResponse<InquiryResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateInquiryStatusRequest request) {
        return ApiResponse.of("Inquiry status updated", inquiryService.updateStatus(id, request.status()));
    }

}
