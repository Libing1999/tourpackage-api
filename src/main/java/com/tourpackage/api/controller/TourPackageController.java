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
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.TourPackagePublicDetailResponse;
import com.tourpackage.api.dto.response.TourPackageSummaryResponse;
import com.tourpackage.api.entity.DifficultyLevel;
import com.tourpackage.api.service.TourPackageService;

@RestController
@RequestMapping("/public/tour-packages")
public class TourPackageController {

    private final TourPackageService tourPackageService;

    public TourPackageController(TourPackageService tourPackageService) {
        this.tourPackageService = tourPackageService;
    }

    @GetMapping("/best")
    public ApiResponse<List<TourPackageSummaryResponse>> getBestPackages(
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.of(tourPackageService.getBestPackages(Math.clamp(limit, 1, 50)));
    }

    @GetMapping("/offers")
    public ApiResponse<List<TourPackageSummaryResponse>> getSpecialOffers(
            @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.of(tourPackageService.getSpecialOffers(Math.clamp(limit, 1, 50)));
    }

    @GetMapping
    public ApiResponse<PageResponse<TourPackageSummaryResponse>> list(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID countryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Short minDurationDays,
            @RequestParam(required = false) Short maxDurationDays,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @RequestParam(defaultValue = "false") boolean discountedOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12) Pageable pageable) {
        return ApiResponse.of(tourPackageService.list(
                cityId, countryId, minPrice, maxPrice, minDurationDays, maxDurationDays,
                difficultyLevel, discountedOnly, search, pageable));
    }

    @GetMapping("/{slug}")
    public ApiResponse<TourPackagePublicDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.of(tourPackageService.getBySlug(slug));
    }

}
