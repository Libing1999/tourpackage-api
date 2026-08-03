package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.TestimonialResponse;
import com.tourpackage.api.service.TestimonialService;

@RestController
@RequestMapping("/public/testimonials")
public class TestimonialController {

    private final TestimonialService testimonialService;

    public TestimonialController(TestimonialService testimonialService) {
        this.testimonialService = testimonialService;
    }

    @GetMapping("/featured")
    public ApiResponse<List<TestimonialResponse>> getFeaturedTestimonials(
            @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.of(testimonialService.getFeaturedTestimonials(Math.clamp(limit, 1, 50)));
    }

}
