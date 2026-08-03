package com.tourpackage.api.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.BannerResponse;
import com.tourpackage.api.entity.BannerPlacement;
import com.tourpackage.api.mapper.BannerMapper;
import com.tourpackage.api.repository.BannerRepository;

@Service
@Transactional(readOnly = true)
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;

    public BannerService(BannerRepository bannerRepository, BannerMapper bannerMapper) {
        this.bannerRepository = bannerRepository;
        this.bannerMapper = bannerMapper;
    }

    /** The homepage slider. Other placements are read through the CMS API. */
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findActiveBanners(BannerPlacement.HOME_SLIDER, Instant.now()).stream()
                .map(bannerMapper::toResponse)
                .toList();
    }

}
