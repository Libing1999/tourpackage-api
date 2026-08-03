package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.DestinationResponse;
import com.tourpackage.api.service.DestinationService;

@RestController
@RequestMapping("/public/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @GetMapping("/popular")
    public ApiResponse<List<DestinationResponse>> getPopularDestinations(
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.of(destinationService.getPopularDestinations(Math.clamp(limit, 1, 50)));
    }

    @GetMapping
    public ApiResponse<List<DestinationResponse>> getAllDestinations() {
        return ApiResponse.of(destinationService.getAllDestinations());
    }

}
