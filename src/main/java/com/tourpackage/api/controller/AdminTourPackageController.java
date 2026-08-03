package com.tourpackage.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.PackageImageRequest;
import com.tourpackage.api.dto.request.PackageItineraryRequest;
import com.tourpackage.api.dto.request.PackageLineItemRequest;
import com.tourpackage.api.dto.request.TourPackageRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.PackageImageResponse;
import com.tourpackage.api.dto.response.PackageItineraryResponse;
import com.tourpackage.api.dto.response.PackageLineItemResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.TourPackageAdminListResponse;
import com.tourpackage.api.dto.response.TourPackageAdminResponse;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.service.TourPackageAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/tour-packages")
@PreAuthorize("isAuthenticated()")
public class AdminTourPackageController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final TourPackageAdminService tourPackageAdminService;

    public AdminTourPackageController(TourPackageAdminService tourPackageAdminService) {
        this.tourPackageAdminService = tourPackageAdminService;
    }

    @GetMapping
    public ApiResponse<PageResponse<TourPackageAdminListResponse>> list(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.of(tourPackageAdminService.list(status, cityId, search, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<TourPackageAdminResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(tourPackageAdminService.getById(id));
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TourPackageAdminResponse> create(@Valid @RequestBody TourPackageRequest request) {
        return ApiResponse.of("Tour package created", tourPackageAdminService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<TourPackageAdminResponse> update(
            @PathVariable UUID id, @Valid @RequestBody TourPackageRequest request) {
        return ApiResponse.of("Tour package updated", tourPackageAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        tourPackageAdminService.delete(id);
        return ApiResponse.message("Tour package deleted");
    }

    @PostMapping("/{id}/images")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageImageResponse> addImage(
            @PathVariable UUID id, @Valid @RequestBody PackageImageRequest request) {
        return ApiResponse.of("Image added", tourPackageAdminService.addImage(id, request));
    }

    @PutMapping("/{id}/images/{imageId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<PackageImageResponse> updateImage(
            @PathVariable UUID id, @PathVariable UUID imageId, @Valid @RequestBody PackageImageRequest request) {
        return ApiResponse.of("Image updated", tourPackageAdminService.updateImage(id, imageId, request));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        tourPackageAdminService.deleteImage(id, imageId);
        return ApiResponse.message("Image deleted");
    }

    @PostMapping("/{id}/itinerary")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageItineraryResponse> addItineraryDay(
            @PathVariable UUID id, @Valid @RequestBody PackageItineraryRequest request) {
        return ApiResponse.of("Itinerary day added", tourPackageAdminService.addItineraryDay(id, request));
    }

    @PutMapping("/{id}/itinerary/{dayId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<PackageItineraryResponse> updateItineraryDay(
            @PathVariable UUID id, @PathVariable UUID dayId, @Valid @RequestBody PackageItineraryRequest request) {
        return ApiResponse.of("Itinerary day updated", tourPackageAdminService.updateItineraryDay(id, dayId, request));
    }

    @DeleteMapping("/{id}/itinerary/{dayId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteItineraryDay(@PathVariable UUID id, @PathVariable UUID dayId) {
        tourPackageAdminService.deleteItineraryDay(id, dayId);
        return ApiResponse.message("Itinerary day deleted");
    }

    @PostMapping("/{id}/includes")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageLineItemResponse> addInclude(
            @PathVariable UUID id, @Valid @RequestBody PackageLineItemRequest request) {
        return ApiResponse.of("Include item added", tourPackageAdminService.addInclude(id, request));
    }

    @DeleteMapping("/{id}/includes/{includeId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteInclude(@PathVariable UUID id, @PathVariable UUID includeId) {
        tourPackageAdminService.deleteInclude(id, includeId);
        return ApiResponse.message("Include item deleted");
    }

    @PostMapping("/{id}/excludes")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageLineItemResponse> addExclude(
            @PathVariable UUID id, @Valid @RequestBody PackageLineItemRequest request) {
        return ApiResponse.of("Exclude item added", tourPackageAdminService.addExclude(id, request));
    }

    @DeleteMapping("/{id}/excludes/{excludeId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteExclude(@PathVariable UUID id, @PathVariable UUID excludeId) {
        tourPackageAdminService.deleteExclude(id, excludeId);
        return ApiResponse.message("Exclude item deleted");
    }

}
