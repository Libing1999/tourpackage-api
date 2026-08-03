package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.dto.response.BlogPostSummaryResponse;
import com.tourpackage.api.entity.BlogPost;
import com.tourpackage.api.entity.ContentStatus;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    @Query("""
            SELECT new com.tourpackage.api.dto.response.BlogPostSummaryResponse(
                b.id, b.title, b.slug, b.excerpt, b.coverImageUrl, b.category,
                b.publishedAt, b.readTimeMinutes, a.fullName)
            FROM BlogPost b
            LEFT JOIN Admin a ON a.id = b.authorId
            WHERE b.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED
            ORDER BY b.publishedAt DESC
            """)
    List<BlogPostSummaryResponse> findRecentPosts(Pageable pageable);

    @Query(value = """
            SELECT new com.tourpackage.api.dto.response.BlogPostSummaryResponse(
                b.id, b.title, b.slug, b.excerpt, b.coverImageUrl, b.category,
                b.publishedAt, b.readTimeMinutes, a.fullName)
            FROM BlogPost b
            LEFT JOIN Admin a ON a.id = b.authorId
            WHERE b.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED
              AND (:category IS NULL OR LOWER(b.category) = LOWER(CAST(:category AS string)))
            ORDER BY b.publishedAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM BlogPost b
            WHERE b.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED
              AND (:category IS NULL OR LOWER(b.category) = LOWER(CAST(:category AS string)))
            """)
    Page<BlogPostSummaryResponse> findPublished(@Param("category") String category, Pageable pageable);

    Optional<BlogPost> findBySlugAndStatus(String slug, ContentStatus status);

    List<BlogPost> findAllByOrderByCreatedAtDesc();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

}
