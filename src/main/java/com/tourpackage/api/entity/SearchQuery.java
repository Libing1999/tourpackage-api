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
 * A distinct search term and how often it has been used.
 *
 * <p>One row per term rather than per search — see {@code V19__global_search.sql}
 * for why the aggregate is kept instead of a log.
 */
@Entity
@Table(name = "search_queries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchQuery {

    @Id
    @GeneratedValue
    private UUID id;

    /** Lowercased and whitespace-collapsed; the unique key. */
    @Column(name = "term", nullable = false, length = 100)
    private String term;

    /** The last spelling a visitor typed, for display. */
    @Column(name = "display_term", nullable = false, length = 100)
    private String displayTerm;

    @Column(name = "search_count", nullable = false)
    private long searchCount;

    @Column(name = "last_searched_at", nullable = false)
    private Instant lastSearchedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
