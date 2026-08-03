package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.FaqResponse;
import com.tourpackage.api.service.FaqService;

@RestController
@RequestMapping("/public/faqs")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @GetMapping
    public ApiResponse<List<FaqResponse>> getActiveFaqs() {
        return ApiResponse.of(faqService.getActiveFaqs());
    }

}
