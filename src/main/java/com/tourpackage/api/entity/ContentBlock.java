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

/**
 * A named piece of editorial copy — a section heading, a page intro. Addressed
 * by a dotted key ({@code home.hotels}, {@code page.contact}) so adding a
 * section is a new row rather than a new column.
 */
@Entity
@Table(name = "content_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentBlock {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "key", nullable = false, length = 100)
    private String key;

    @Column(name = "eyebrow", length = 100)
    private String eyebrow;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "body")
    private String body;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
