package com.tourpackage.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.BannerRequest;
import com.tourpackage.api.dto.request.BlogPostRequest;
import com.tourpackage.api.dto.request.ContentBlockRequest;
import com.tourpackage.api.dto.request.GalleryImageRequest;
import com.tourpackage.api.dto.request.NavLinkRequest;
import com.tourpackage.api.dto.request.PageSeoRequest;
import com.tourpackage.api.dto.request.ReorderRequest;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.BannerAdminResponse;
import com.tourpackage.api.dto.response.BlogPostAdminResponse;
import com.tourpackage.api.dto.response.ContentBlockResponse;
import com.tourpackage.api.dto.response.GalleryImageResponse;
import com.tourpackage.api.dto.response.NavLinkResponse;
import com.tourpackage.api.dto.response.PageSeoResponse;
import com.tourpackage.api.security.AuthenticatedAdmin;
import com.tourpackage.api.service.CmsService;

import jakarta.validation.Valid;

/**
 * All six CMS content types under one controller. They share an identical
 * shape — list, create, update, delete, all gated the same way — so splitting
 * them into six near-identical classes would add files without adding clarity.
 */
@RestController
@RequestMapping("/admin/cms")
@PreAuthorize("isAuthenticated()")
public class AdminCmsController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final CmsService cmsService;

    public AdminCmsController(CmsService cmsService) {
        this.cmsService = cmsService;
    }

    // --- content blocks ---

    @GetMapping("/blocks")
    public ApiResponse<List<ContentBlockResponse>> listBlocks() {
        return ApiResponse.of(cmsService.listBlocks());
    }

    @PostMapping("/blocks")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContentBlockResponse> createBlock(@Valid @RequestBody ContentBlockRequest request) {
        return ApiResponse.of("Content block created", cmsService.saveBlock(null, request));
    }

    @PutMapping("/blocks/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<ContentBlockResponse> updateBlock(
            @PathVariable UUID id, @Valid @RequestBody ContentBlockRequest request) {
        return ApiResponse.of("Content block updated", cmsService.saveBlock(id, request));
    }

    @DeleteMapping("/blocks/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteBlock(@PathVariable UUID id) {
        cmsService.deleteBlock(id);
        return ApiResponse.message("Content block deleted");
    }

    // --- page SEO ---

    @GetMapping("/seo")
    public ApiResponse<List<PageSeoResponse>> listSeo() {
        return ApiResponse.of(cmsService.listSeo());
    }

    @PostMapping("/seo")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PageSeoResponse> createSeo(@Valid @RequestBody PageSeoRequest request) {
        return ApiResponse.of("Page SEO created", cmsService.saveSeo(null, request));
    }

    @PutMapping("/seo/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<PageSeoResponse> updateSeo(
            @PathVariable UUID id, @Valid @RequestBody PageSeoRequest request) {
        return ApiResponse.of("Page SEO updated", cmsService.saveSeo(id, request));
    }

    @DeleteMapping("/seo/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteSeo(@PathVariable UUID id) {
        cmsService.deleteSeo(id);
        return ApiResponse.message("Page SEO deleted");
    }

    // --- nav links ---

    @GetMapping("/nav-links")
    public ApiResponse<List<NavLinkResponse>> listNavLinks() {
        return ApiResponse.of(cmsService.listNavLinks());
    }

    @PostMapping("/nav-links")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NavLinkResponse> createNavLink(@Valid @RequestBody NavLinkRequest request) {
        return ApiResponse.of("Link created", cmsService.saveNavLink(null, request));
    }

    @PutMapping("/nav-links/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<NavLinkResponse> updateNavLink(
            @PathVariable UUID id, @Valid @RequestBody NavLinkRequest request) {
        return ApiResponse.of("Link updated", cmsService.saveNavLink(id, request));
    }

    @DeleteMapping("/nav-links/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteNavLink(@PathVariable UUID id) {
        cmsService.deleteNavLink(id);
        return ApiResponse.message("Link deleted");
    }

    // --- gallery ---

    @GetMapping("/gallery")
    public ApiResponse<List<GalleryImageResponse>> listGallery() {
        return ApiResponse.of(cmsService.listGalleryImages());
    }

    @PostMapping("/gallery")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GalleryImageResponse> createGalleryImage(@Valid @RequestBody GalleryImageRequest request) {
        return ApiResponse.of("Image added", cmsService.saveGalleryImage(null, request));
    }

    @PutMapping("/gallery/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<GalleryImageResponse> updateGalleryImage(
            @PathVariable UUID id, @Valid @RequestBody GalleryImageRequest request) {
        return ApiResponse.of("Image updated", cmsService.saveGalleryImage(id, request));
    }

    @DeleteMapping("/gallery/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteGalleryImage(@PathVariable UUID id) {
        cmsService.deleteGalleryImage(id);
        return ApiResponse.message("Image deleted");
    }

    /** Drag-to-reorder: the client sends the ids in their new order. */
    @PatchMapping("/gallery/reorder")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<List<GalleryImageResponse>> reorderGallery(@Valid @RequestBody ReorderRequest request) {
        return ApiResponse.of("Order saved", cmsService.reorderGallery(request.ids()));
    }

    // --- banners (homepage slider, offers strip, page heroes) ---

    @GetMapping("/banners")
    public ApiResponse<List<BannerAdminResponse>> listBanners() {
        return ApiResponse.of(cmsService.listBanners());
    }

    @PostMapping("/banners")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BannerAdminResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.of("Banner created", cmsService.saveBanner(null, request));
    }

    @PutMapping("/banners/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<BannerAdminResponse> updateBanner(
            @PathVariable UUID id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.of("Banner updated", cmsService.saveBanner(id, request));
    }

    @DeleteMapping("/banners/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deleteBanner(@PathVariable UUID id) {
        cmsService.deleteBanner(id);
        return ApiResponse.message("Banner deleted");
    }

    // --- blog posts ---

    @GetMapping("/blog")
    public ApiResponse<List<BlogPostAdminResponse>> listPosts() {
        return ApiResponse.of(cmsService.listPosts());
    }

    @PostMapping("/blog")
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BlogPostAdminResponse> createPost(
            @AuthenticationPrincipal AuthenticatedAdmin principal,
            @Valid @RequestBody BlogPostRequest request) {
        return ApiResponse.of("Post created", cmsService.savePost(null, request, principal.id()));
    }

    @PutMapping("/blog/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<BlogPostAdminResponse> updatePost(
            @AuthenticationPrincipal AuthenticatedAdmin principal,
            @PathVariable UUID id,
            @Valid @RequestBody BlogPostRequest request) {
        return ApiResponse.of("Post updated", cmsService.savePost(id, request, principal.id()));
    }

    @DeleteMapping("/blog/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> deletePost(@PathVariable UUID id) {
        cmsService.deletePost(id);
        return ApiResponse.message("Post deleted");
    }

}
