package com.tourpackage.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.entity.SearchQuery;

/**
 * Search across hotels, packages, cities and countries.
 *
 * <p>Every query here is native. JPQL has no {@code ILIKE}, no trigram operators
 * and no way to express a UNION, and all three are load-bearing:
 *
 * <ul>
 *   <li><strong>{@code ILIKE}, not {@code LOWER(x) LIKE LOWER(y)}.</strong> The
 *       trigram indexes are on the columns; wrapping the column in {@code LOWER()}
 *       makes them unusable. Measured on 200k rows, the difference is a 54.6ms
 *       parallel sequential scan against a 6.1ms bitmap index scan.</li>
 *   <li><strong>{@code <%} (word similarity)</strong> makes the match itself
 *       typo-tolerant. It is the word-wise operator rather than plain {@code %}
 *       because a short query is compared against multi-word names: for
 *       "serenty" against "Bali Serenity Villas", whole-string similarity is
 *       0.261 and falls below the 0.3 default threshold, while word similarity
 *       is 0.625 and matches. The same trigram index serves both.</li>
 *   <li><strong>A UNION</strong> lets one page of results interleave types by
 *       relevance. Four separate paginated queries could not.</li>
 * </ul>
 *
 * <p>{@code :type} is never null — callers pass {@code 'ALL'} rather than leaving
 * it out. PostgreSQL cannot infer the type of a null bind parameter and reports
 * it as {@code could not determine data type of parameter}, which this codebase
 * has hit more than once; a sentinel avoids the question entirely. It also lets
 * the planner fold the unselected branches to a one-time false filter instead of
 * scanning them.
 */
public interface SearchRepository extends JpaRepository<SearchQuery, java.util.UUID> {

    /**
     * The four branches, shared by the page and count queries.
     *
     * <p>Ranking is deliberately coarse — exact title, then prefix, then
     * anything else — with trigram similarity breaking ties inside each band.
     * A visitor typing "bali" expects Bali first and does not care how the rest
     * are ordered among themselves.
     */
    String UNION_SQL = """
            SELECT 'HOTEL' AS type, h.id::text AS id, h.name AS title,
                   c.name || ', ' || co.name AS subtitle,
                   (SELECT hi.url FROM hotel_images hi WHERE hi.hotel_id = h.id
                     ORDER BY hi.is_cover DESC, hi.display_order LIMIT 1) AS image,
                   h.slug AS slug,
                   CASE WHEN lower(h.name) = lower(:q) THEN 100
                        WHEN h.name ILIKE :prefix THEN 50 ELSE 10 END
                     + word_similarity(:q, h.name) AS rank
              FROM hotels h
              JOIN cities c    ON c.id = h.city_id
              JOIN countries co ON co.id = c.country_id
             WHERE h.status = 'PUBLISHED' AND h.deleted_at IS NULL
               AND (:type = 'ALL' OR :type = 'HOTEL')
               AND (h.name ILIKE :pattern OR :q <% h.name)

            UNION ALL

            SELECT 'PACKAGE', p.id::text, p.title,
                   c.name || ', ' || co.name,
                   (SELECT pi.url FROM package_images pi WHERE pi.package_id = p.id
                     ORDER BY pi.is_cover DESC, pi.display_order LIMIT 1),
                   p.slug,
                   CASE WHEN lower(p.title) = lower(:q) THEN 100
                        WHEN p.title ILIKE :prefix THEN 50 ELSE 10 END
                     + word_similarity(:q, p.title)
              FROM tour_packages p
              JOIN cities c     ON c.id = p.city_id
              JOIN countries co ON co.id = p.country_id
             WHERE p.status = 'PUBLISHED' AND p.deleted_at IS NULL
               AND (:type = 'ALL' OR :type = 'PACKAGE')
               AND (p.title ILIKE :pattern OR :q <% p.title)

            UNION ALL

            SELECT 'CITY', ci.id::text, ci.name,
                   co.name,
                   ci.image_url,
                   ci.slug,
                   CASE WHEN lower(ci.name) = lower(:q) THEN 100
                        WHEN ci.name ILIKE :prefix THEN 50 ELSE 10 END
                     + word_similarity(:q, ci.name)
              FROM cities ci
              JOIN countries co ON co.id = ci.country_id
             WHERE ci.is_active
               AND (:type = 'ALL' OR :type = 'CITY')
               AND (ci.name ILIKE :pattern OR :q <% ci.name)

            UNION ALL

            SELECT 'COUNTRY', co.id::text, co.name,
                   (SELECT count(*) || ' destination(s)' FROM cities ci2
                     WHERE ci2.country_id = co.id AND ci2.is_active),
                   NULL,
                   NULL,
                   CASE WHEN lower(co.name) = lower(:q) THEN 100
                        WHEN co.name ILIKE :prefix THEN 50 ELSE 10 END
                     + word_similarity(:q, co.name)
              FROM countries co
             WHERE co.is_active
               AND (:type = 'ALL' OR :type = 'COUNTRY')
               AND (co.name ILIKE :pattern OR :q <% co.name)
            """;

    /**
     * One page of results across every selected type, most relevant first.
     *
     * <p>{@code title} is the final tiebreaker so that equally-ranked rows keep a
     * stable order between pages — without it, page 2 can repeat a row page 1
     * already showed.
     */
    @Query(value = "SELECT * FROM (" + UNION_SQL + ") r ORDER BY r.rank DESC, r.title ASC",
            countQuery = "SELECT count(*) FROM (" + UNION_SQL + ") r",
            nativeQuery = true)
    Page<SearchHitProjection> search(
            @Param("q") String q,
            @Param("pattern") String pattern,
            @Param("prefix") String prefix,
            @Param("type") String type,
            Pageable pageable);

    /**
     * Autocomplete for one type.
     *
     * <p>Separate from {@link #search} so each type gets its own small limit —
     * a single ranked top-ten can legitimately be ten hotels, which hides from
     * the visitor that packages and destinations matched too.
     */
    @Query(value = "SELECT * FROM (" + UNION_SQL + ") r ORDER BY r.rank DESC, r.title ASC LIMIT :max",
            nativeQuery = true)
    List<SearchHitProjection> suggest(
            @Param("q") String q,
            @Param("pattern") String pattern,
            @Param("prefix") String prefix,
            @Param("type") String type,
            @Param("max") int max);

    /**
     * Records a search, or bumps the count if the term has been used before.
     *
     * <p>An upsert rather than select-then-insert: searches are concurrent by
     * nature, and two visitors typing the same term at once would otherwise race
     * on the unique index. {@code display_term} is overwritten so the list
     * reflects how people most recently spelled it.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            INSERT INTO search_queries (term, display_term, search_count, last_searched_at, created_at)
            VALUES (:term, :displayTerm, 1, now(), now())
            ON CONFLICT (term) DO UPDATE
               SET search_count     = search_queries.search_count + 1,
                   display_term     = EXCLUDED.display_term,
                   last_searched_at = now()
            """, nativeQuery = true)
    void recordSearch(@Param("term") String term, @Param("displayTerm") String displayTerm);

    /** Most-used terms. Excludes one-off searches — a term used once is noise. */
    @Query(value = """
            SELECT display_term AS term, search_count AS count
              FROM search_queries
             WHERE search_count > 1
             ORDER BY search_count DESC, last_searched_at DESC
             LIMIT :max
            """, nativeQuery = true)
    List<PopularTermProjection> findPopular(@Param("max") int max);

    /** Native-query projection; column aliases map to these getters. */
    interface SearchHitProjection {
        String getType();

        String getId();

        String getTitle();

        String getSubtitle();

        String getImage();

        String getSlug();
    }

    interface PopularTermProjection {
        String getTerm();

        long getCount();
    }

}
