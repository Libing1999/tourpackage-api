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

import com.tourpackage.api.dto.request.UpdateBookingStatusRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.BookingAdminListResponse;
import com.tourpackage.api.dto.response.BookingResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.BookingStatus;
import com.tourpackage.api.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/bookings")
@PreAuthorize("isAuthenticated()")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingAdminListResponse>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.of(bookingService.list(status, search, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(bookingService.getById(id));
    }

    /**
     * Confirm / cancel / complete. SUPPORT is included alongside the content
     * roles here — unlike hotel or package content, working a booking queue is
     * exactly what a support role exists to do.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR','SUPPORT')")
    public ApiResponse<BookingResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateBookingStatusRequest request) {
        return ApiResponse.of("Booking status updated",
                bookingService.updateStatus(id, request.status(), request.cancellationReason()));
    }

}
