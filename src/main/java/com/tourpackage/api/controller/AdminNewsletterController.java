package com.tourpackage.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.NewsletterSubscriberResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.service.AdminContentService;

@RestController
@RequestMapping("/admin/newsletter")
@PreAuthorize("isAuthenticated()")
public class AdminNewsletterController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final AdminContentService adminContentService;

    public AdminNewsletterController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NewsletterSubscriberResponse>> list(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.of(adminContentService.listSubscribers(active, pageable));
    }

    /** DELETE, but a deactivation — see {@code AdminContentService.unsubscribe}. */
    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> unsubscribe(@PathVariable UUID id) {
        adminContentService.unsubscribe(id);
        return ApiResponse.message("Subscriber unsubscribed");
    }

}
