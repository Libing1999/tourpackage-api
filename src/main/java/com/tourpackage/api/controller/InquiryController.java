package com.tourpackage.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.CreateInquiryRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.service.InquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/public/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@Valid @RequestBody CreateInquiryRequest request) {
        inquiryService.create(request);
        // Nothing from the saved row is echoed back: the visitor has no way to
        // look an enquiry up afterwards, so an id would only be noise.
        return ApiResponse.message("Thanks for getting in touch — we'll reply shortly.");
    }

}
