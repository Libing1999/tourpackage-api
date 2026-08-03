package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.BannerAdminResponse;
import com.tourpackage.api.dto.response.BlogPostDetailResponse;
import com.tourpackage.api.dto.response.BlogPostSummaryResponse;
import com.tourpackage.api.dto.response.GalleryImageResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.PageSeoResponse;
import com.tourpackage.api.dto.response.SiteContentResponse;
import com.tourpackage.api.entity.BannerPlacement;
import com.tourpackage.api.service.CmsService;

/** What the public site reads. Everything here is editable in the admin CMS. */
@RestController
@RequestMapping("/public/cms")
public class CmsController {

    private final CmsService cmsService;

    public CmsController(CmsService cmsService) {
        this.cmsService = cmsService;
    }

    /** Section headings and navigation in one call — both are needed on every
     * page, so this is fetched once and cached rather than per-section. */
    @GetMapping("/site-content")
    public ApiResponse<SiteContentResponse> getSiteContent() {
        return ApiResponse.of(cmsService.getSiteContent());
    }

    /** Null data when the path has no managed metadata — the page falls back
     * to its own defaults rather than failing. */
    @GetMapping("/seo")
    public ApiResponse<PageSeoResponse> getSeo(@RequestParam String path) {
        return ApiResponse.of(cmsService.getSeoForPath(path));
    }

    @GetMapping("/gallery")
    public ApiResponse<List<GalleryImageResponse>> getGallery() {
        return ApiResponse.of(cmsService.getGallery());
    }

    @GetMapping("/banners")
    public ApiResponse<List<BannerAdminResponse>> getBanners(
            @RequestParam(defaultValue = "HOME_SLIDER") BannerPlacement placement) {
        return ApiResponse.of(cmsService.getBannersByPlacement(placement));
    }

    @GetMapping("/blog")
    public ApiResponse<PageResponse<BlogPostSummaryResponse>> listPosts(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 9) Pageable pageable) {
        return ApiResponse.of(cmsService.listPublishedPosts(category, pageable));
    }

    @GetMapping("/blog/{slug}")
    public ApiResponse<BlogPostDetailResponse> getPost(@PathVariable String slug) {
        return ApiResponse.of(cmsService.getPostBySlug(slug));
    }

}
