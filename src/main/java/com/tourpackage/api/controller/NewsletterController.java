package com.tourpackage.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.NewsletterSubscribeRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.service.NewsletterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/public/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.subscribe(request.email());
        return ApiResponse.message("Subscribed! Watch your inbox for travel inspiration.");
    }

}
