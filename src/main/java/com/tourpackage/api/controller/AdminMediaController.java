package com.tourpackage.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.MediaAssetResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.security.AuthenticatedAdmin;
import com.tourpackage.api.service.MediaService;

@RestController
@RequestMapping("/admin/media")
@PreAuthorize("isAuthenticated()")
public class AdminMediaController {

    private static final String CAN_MANAGE = "hasAnyRole('SUPER_ADMIN','ADMIN','EDITOR')";

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MediaAssetResponse>> list(
            @RequestParam(required = false) String folder,
            @PageableDefault(size = 24) Pageable pageable) {
        return ApiResponse.of(mediaService.list(folder, pageable));
    }

    /** Multipart, several files under the same {@code files} part. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CAN_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<MediaAssetResponse>> upload(
            @AuthenticationPrincipal AuthenticatedAdmin principal,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "general") String folder) {
        List<MediaAssetResponse> uploaded = mediaService.upload(files, folder, principal.id());
        return ApiResponse.of(uploaded.size() + " image(s) uploaded", uploaded);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        mediaService.delete(id);
        return ApiResponse.message("Image deleted");
    }

}
