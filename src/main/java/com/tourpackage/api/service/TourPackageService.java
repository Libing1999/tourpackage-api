package com.tourpackage.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.TourPackagePublicDetailResponse;
import com.tourpackage.api.dto.response.TourPackageSummaryResponse;
import com.tourpackage.api.entity.City;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.Country;
import com.tourpackage.api.entity.DifficultyLevel;
import com.tourpackage.api.entity.TourPackage;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.CityRepository;
import com.tourpackage.api.repository.CountryRepository;
import com.tourpackage.api.repository.TourPackageRepository;

@Service
@Transactional(readOnly = true)
public class TourPackageService {

    private final TourPackageRepository tourPackageRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final TourPackageAdminService tourPackageAdminService;
    private final PackagePricing packagePricing;

    public TourPackageService(
            TourPackageRepository tourPackageRepository,
            CityRepository cityRepository,
            CountryRepository countryRepository,
            TourPackageAdminService tourPackageAdminService,
            PackagePricing packagePricing) {
        this.tourPackageRepository = tourPackageRepository;
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
        this.tourPackageAdminService = tourPackageAdminService;
        this.packagePricing = packagePricing;
    }

    public List<TourPackageSummaryResponse> getBestPackages(int limit) {
        return tourPackageRepository.findBestPackages(PageRequest.of(0, limit));
    }

    public List<TourPackageSummaryResponse> getSpecialOffers(int limit) {
        return tourPackageRepository.findSpecialOffers(PageRequest.of(0, limit));
    }

    public PageResponse<TourPackageSummaryResponse> list(
            UUID cityId,
            UUID countryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Short minDurationDays,
            Short maxDurationDays,
            DifficultyLevel difficultyLevel,
            boolean discountedOnly,
            String search,
            Pageable pageable) {
        return PageResponse.of(tourPackageRepository.searchPublic(
                cityId, countryId, minPrice, maxPrice, minDurationDays, maxDurationDays,
                difficultyLevel, discountedOnly, search, pageable));
    }

    public TourPackagePublicDetailResponse getBySlug(String slug) {
        TourPackage tourPackage = tourPackageRepository
                .findBySlugAndDeletedAtIsNullAndStatus(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package not found: " + slug));

        City city = cityRepository.findById(tourPackage.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + tourPackage.getCityId()));
        Country country = countryRepository.findById(tourPackage.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + tourPackage.getCountryId()));

        return new TourPackagePublicDetailResponse(
                tourPackage.getId(),
                tourPackage.getTitle(),
                tourPackage.getSlug(),
                tourPackage.getSummary(),
                tourPackage.getDescription(),
                country.getName(),
                city.getName(),
                tourPackage.getDurationDays(),
                tourPackage.getDurationNights(),
                tourPackage.getPrice(),
                tourPackage.getDiscountPrice(),
                packagePricing.pricePerAdult(tourPackage),
                packagePricing.pricePerChild(tourPackage),
                tourPackage.getCurrencyCode(),
                tourPackage.getMinGroupSize(),
                tourPackage.getMaxGroupSize(),
                tourPackage.getDifficultyLevel(),
                tourPackage.getRatingAverage(),
                tourPackage.getRatingCount(),
                tourPackage.getMetaTitle(),
                tourPackage.getMetaDescription(),
                tourPackageAdminService.buildImages(tourPackage.getId()),
                tourPackageAdminService.buildItinerary(tourPackage.getId()),
                tourPackageAdminService.buildIncludes(tourPackage.getId()),
                tourPackageAdminService.buildExcludes(tourPackage.getId()));
    }

}
