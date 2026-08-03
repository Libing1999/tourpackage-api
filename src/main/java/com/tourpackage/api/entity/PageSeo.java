package com.tourpackage.api.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** SEO metadata for one route. Detail pages derive their own title from the
 * entity they render, so only listing and static routes live here. */
@Entity
@Table(name = "page_seo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageSeo {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "path", nullable = false, length = 200)
    private String path;

    @Column(name = "meta_title", nullable = false, length = 200)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "og_image_url")
    private String ogImageUrl;

    @Column(name = "no_index", nullable = false)
    private boolean noIndex;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
