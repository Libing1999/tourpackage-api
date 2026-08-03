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

import com.tourpackage.api.dto.request.AmenityRequest;
import com.tourpackage.api.dto.response.AmenityResponse;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.service.AmenityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/amenities")
@PreAuthorize("isAuthenticated()")
public class AdminAmenityController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final AmenityService amenityService;

    public AdminAmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping
    public ApiResponse<List<AmenityResponse>> list() {
        return ApiResponse.of(amenityService.list());
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AmenityResponse> create(@Valid @RequestBody AmenityRequest request) {
        return ApiResponse.of("Amenity created", amenityService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<AmenityResponse> update(@PathVariable UUID id, @Valid @RequestBody AmenityRequest request) {
        return ApiResponse.of("Amenity updated", amenityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        amenityService.delete(id);
        return ApiResponse.message("Amenity deleted");
    }

}
