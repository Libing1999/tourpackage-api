package com.tourpackage.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.HotelPublicDetailResponse;
import com.tourpackage.api.dto.response.HotelSummaryResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.service.HotelService;

@RestController
@RequestMapping("/public/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/top")
    public ApiResponse<List<HotelSummaryResponse>> getTopHotels(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.of(hotelService.getTopHotels(Math.clamp(limit, 1, 50)));
    }

    @GetMapping
    public ApiResponse<PageResponse<HotelSummaryResponse>> list(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID countryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Short minStarRating,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<UUID> amenityIds,
            @PageableDefault(size = 12) Pageable pageable) {
        return ApiResponse.of(
                hotelService.list(cityId, countryId, minPrice, maxPrice, minStarRating, search, amenityIds, pageable));
    }

    @GetMapping("/{slug}")
    public ApiResponse<HotelPublicDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.of(hotelService.getBySlug(slug));
    }

}
