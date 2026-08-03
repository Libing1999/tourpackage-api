package com.tourpackage.api.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.TestimonialResponse;
import com.tourpackage.api.repository.TestimonialRepository;

@Service
@Transactional(readOnly = true)
public class TestimonialService {

    private final TestimonialRepository testimonialRepository;

    public TestimonialService(TestimonialRepository testimonialRepository) {
        this.testimonialRepository = testimonialRepository;
    }

    public List<TestimonialResponse> getFeaturedTestimonials(int limit) {
        return testimonialRepository.findFeaturedTestimonials(PageRequest.of(0, limit));
    }

}
