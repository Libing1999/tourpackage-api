package com.tourpackage.api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.CustomerResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.service.AdminContentService;

/**
 * Read-only: customers are created by the booking flow, never by staff, so
 * there's no create or edit here. Deleting one would orphan their bookings,
 * which {@code fk_bookings_user ON DELETE RESTRICT} already forbids.
 */
@RestController
@RequestMapping("/admin/customers")
@PreAuthorize("isAuthenticated()")
public class AdminCustomerController {

    private final AdminContentService adminContentService;

    public AdminCustomerController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.of(adminContentService.listCustomers(search, pageable));
    }

}
