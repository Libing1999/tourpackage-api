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

import com.tourpackage.api.dto.request.HotelImageRequest;
import com.tourpackage.api.dto.request.HotelRequest;
import com.tourpackage.api.dto.request.HotelRoomRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.HotelAdminListResponse;
import com.tourpackage.api.dto.response.HotelAdminResponse;
import com.tourpackage.api.dto.response.HotelImageResponse;
import com.tourpackage.api.dto.response.HotelRoomResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.service.HotelAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/hotels")
@PreAuthorize("isAuthenticated()")
public class AdminHotelController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final HotelAdminService hotelAdminService;

    public AdminHotelController(HotelAdminService hotelAdminService) {
        this.hotelAdminService = hotelAdminService;
    }

    @GetMapping
    public ApiResponse<PageResponse<HotelAdminListResponse>> list(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.of(hotelAdminService.list(status, cityId, search, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelAdminResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(hotelAdminService.getById(id));
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HotelAdminResponse> create(@Valid @RequestBody HotelRequest request) {
        return ApiResponse.of("Hotel created", hotelAdminService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<HotelAdminResponse> update(@PathVariable UUID id, @Valid @RequestBody HotelRequest request) {
        return ApiResponse.of("Hotel updated", hotelAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        hotelAdminService.delete(id);
        return ApiResponse.message("Hotel deleted");
    }

    @PostMapping("/{id}/images")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HotelImageResponse> addImage(@PathVariable UUID id, @Valid @RequestBody HotelImageRequest request) {
        return ApiResponse.of("Image added", hotelAdminService.addImage(id, request));
    }

    @PutMapping("/{id}/images/{imageId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<HotelImageResponse> updateImage(
            @PathVariable UUID id, @PathVariable UUID imageId, @Valid @RequestBody HotelImageRequest request) {
        return ApiResponse.of("Image updated", hotelAdminService.updateImage(id, imageId, request));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        hotelAdminService.deleteImage(id, imageId);
        return ApiResponse.message("Image deleted");
    }

    @PostMapping("/{id}/rooms")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HotelRoomResponse> addRoom(@PathVariable UUID id, @Valid @RequestBody HotelRoomRequest request) {
        return ApiResponse.of("Room added", hotelAdminService.addRoom(id, request));
    }

    @PutMapping("/{id}/rooms/{roomId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<HotelRoomResponse> updateRoom(
            @PathVariable UUID id, @PathVariable UUID roomId, @Valid @RequestBody HotelRoomRequest request) {
        return ApiResponse.of("Room updated", hotelAdminService.updateRoom(id, roomId, request));
    }

    @DeleteMapping("/{id}/rooms/{roomId}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteRoom(@PathVariable UUID id, @PathVariable UUID roomId) {
        hotelAdminService.deleteRoom(id, roomId);
        return ApiResponse.message("Room deleted");
    }

}
