package com.tourpackage.api.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.config.CacheConfig;
import com.tourpackage.api.dto.response.FaqResponse;
import com.tourpackage.api.mapper.FaqMapper;
import com.tourpackage.api.repository.FaqRepository;

@Service
@Transactional(readOnly = true)
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqMapper faqMapper;

    public FaqService(FaqRepository faqRepository, FaqMapper faqMapper) {
        this.faqRepository = faqRepository;
        this.faqMapper = faqMapper;
    }

    @Cacheable(CacheConfig.FAQS)
    public List<FaqResponse> getActiveFaqs() {
        return faqRepository.findByActiveTrueOrderByCategoryAscDisplayOrderAsc().stream()
                .map(faqMapper::toResponse)
                .toList();
    }

}
