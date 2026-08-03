package com.tourpackage.api.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.BlogPostSummaryResponse;
import com.tourpackage.api.repository.BlogPostRepository;

@Service
@Transactional(readOnly = true)
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;

    public BlogPostService(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    public List<BlogPostSummaryResponse> getRecentPosts(int limit) {
        return blogPostRepository.findRecentPosts(PageRequest.of(0, limit));
    }

}
