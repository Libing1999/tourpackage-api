package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.AmenityResponse;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.service.AmenityService;

@RestController
@RequestMapping("/public/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @GetMapping
    public ApiResponse<List<AmenityResponse>> list() {
        return ApiResponse.of(amenityService.listActive());
    }

}
