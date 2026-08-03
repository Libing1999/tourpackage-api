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

import com.tourpackage.api.dto.request.TestimonialRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.TestimonialAdminResponse;
import com.tourpackage.api.service.AdminContentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/testimonials")
@PreAuthorize("isAuthenticated()")
public class AdminTestimonialController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final AdminContentService adminContentService;

    public AdminTestimonialController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping
    public ApiResponse<List<TestimonialAdminResponse>> list() {
        return ApiResponse.of(adminContentService.listTestimonials());
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TestimonialAdminResponse> create(@Valid @RequestBody TestimonialRequest request) {
        return ApiResponse.of("Testimonial created", adminContentService.createTestimonial(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<TestimonialAdminResponse> update(
            @PathVariable UUID id, @Valid @RequestBody TestimonialRequest request) {
        return ApiResponse.of("Testimonial updated", adminContentService.updateTestimonial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        adminContentService.deleteTestimonial(id);
        return ApiResponse.message("Testimonial deleted");
    }

}
