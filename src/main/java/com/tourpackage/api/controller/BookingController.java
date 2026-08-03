package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.CreateHotelBookingRequest;
import com.tourpackage.api.dto.request.CreatePackageBookingRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.BookingResponse;
import com.tourpackage.api.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/public/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/hotel")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> createHotelBooking(@Valid @RequestBody CreateHotelBookingRequest request) {
        return ApiResponse.of("Booking created", bookingService.createHotelBooking(request));
    }

    @PostMapping("/package")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> createPackageBooking(@Valid @RequestBody CreatePackageBookingRequest request) {
        return ApiResponse.of("Booking created", bookingService.createPackageBooking(request));
    }

    /** Every booking made with this email, once the caller proves they hold one
     * valid reference for it — see {@code BookingService.getHistory}. */
    @GetMapping("/history")
    public ApiResponse<List<BookingResponse>> getHistory(
            @RequestParam String bookingNumber,
            @RequestParam String email) {
        return ApiResponse.of(bookingService.getHistory(bookingNumber, email));
    }

    /** Guests have no login, so a booking is retrieved with its number plus the
     * email it was made with — see {@code BookingService.getByBookingNumber}. */
    @GetMapping("/{bookingNumber}")
    public ApiResponse<BookingResponse> getByBookingNumber(
            @PathVariable String bookingNumber,
            @RequestParam String email) {
        return ApiResponse.of(bookingService.getByBookingNumber(bookingNumber, email));
    }

}
