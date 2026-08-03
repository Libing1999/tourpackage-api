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

import com.tourpackage.api.dto.request.RoomTypeRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.RoomTypeResponse;
import com.tourpackage.api.service.RoomTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/room-types")
@PreAuthorize("isAuthenticated()")
public class AdminRoomTypeController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final RoomTypeService roomTypeService;

    public AdminRoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public ApiResponse<List<RoomTypeResponse>> list() {
        return ApiResponse.of(roomTypeService.list());
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomTypeResponse> create(@Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.of("Room type created", roomTypeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<RoomTypeResponse> update(@PathVariable UUID id, @Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.of("Room type updated", roomTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        roomTypeService.delete(id);
        return ApiResponse.message("Room type deleted");
    }

}
