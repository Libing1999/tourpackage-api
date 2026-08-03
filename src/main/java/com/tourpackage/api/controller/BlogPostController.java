package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.BlogPostSummaryResponse;
import com.tourpackage.api.service.BlogPostService;

@RestController
@RequestMapping("/public/blog-posts")
public class BlogPostController {

    private final BlogPostService blogPostService;

    public BlogPostController(BlogPostService blogPostService) {
        this.blogPostService = blogPostService;
    }

    @GetMapping("/recent")
    public ApiResponse<List<BlogPostSummaryResponse>> getRecentPosts(
            @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.of(blogPostService.getRecentPosts(Math.clamp(limit, 1, 50)));
    }

}
