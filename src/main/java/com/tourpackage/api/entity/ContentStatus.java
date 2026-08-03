package com.tourpackage.api.entity;

/**
 * Shared publication-status domain for hotels, tour packages, and blog
 * posts — all three use the identical DRAFT/PUBLISHED/ARCHIVED lifecycle.
 */
public enum ContentStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
