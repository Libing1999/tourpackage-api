package com.tourpackage.api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.dto.response.HotelAdminListResponse;
import com.tourpackage.api.dto.response.HotelSummaryResponse;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    @Query("""
            SELECT new com.tourpackage.api.dto.response.HotelSummaryResponse(
                h.id, h.name, h.slug, c.name, co.name, hi.url,
                h.starRating, h.ratingAverage, h.ratingCount, h.basePrice, h.currencyCode)
            FROM Hotel h
            JOIN City c ON c.id = h.cityId
            JOIN Country co ON co.id = c.countryId
            LEFT JOIN HotelImage hi ON hi.hotelId = h.id AND hi.cover = true
            WHERE h.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND h.deletedAt IS NULL
            ORDER BY h.ratingAverage DESC, h.ratingCount DESC
            """)
    List<HotelSummaryResponse> findTopHotels(Pageable pageable);

    long countByStatusAndDeletedAtIsNull(ContentStatus status);

    Optional<Hotel> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Hotel> findBySlugAndDeletedAtIsNullAndStatus(String slug, ContentStatus status);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndIdNotAndDeletedAtIsNull(String slug, UUID id);

    String ADMIN_LIST_SELECT = """
            SELECT new com.tourpackage.api.dto.response.HotelAdminListResponse(
                h.id, h.name, h.slug, c.name, co.name, hi.url,
                h.starRating, h.basePrice, h.currencyCode, h.ratingAverage, h.ratingCount,
                h.featured, h.status)
            FROM Hotel h
            JOIN City c ON c.id = h.cityId
            JOIN Country co ON co.id = c.countryId
            LEFT JOIN HotelImage hi ON hi.hotelId = h.id AND hi.cover = true
            WHERE h.deletedAt IS NULL
              AND (:status IS NULL OR h.status = :status)
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:search IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    String ADMIN_COUNT_SELECT = """
            SELECT COUNT(h) FROM Hotel h
            JOIN City c ON c.id = h.cityId
            WHERE h.deletedAt IS NULL
              AND (:status IS NULL OR h.status = :status)
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:search IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    @Query(value = ADMIN_LIST_SELECT, countQuery = ADMIN_COUNT_SELECT)
    Page<HotelAdminListResponse> searchAdmin(
            @Param("status") ContentStatus status,
            @Param("cityId") UUID cityId,
            @Param("search") String search,
            Pageable pageable);

    String PUBLIC_LIST_SELECT = """
            SELECT new com.tourpackage.api.dto.response.HotelSummaryResponse(
                h.id, h.name, h.slug, c.name, co.name, hi.url,
                h.starRating, h.ratingAverage, h.ratingCount, h.basePrice, h.currencyCode)
            FROM Hotel h
            JOIN City c ON c.id = h.cityId
            JOIN Country co ON co.id = c.countryId
            LEFT JOIN HotelImage hi ON hi.hotelId = h.id AND hi.cover = true
            WHERE h.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND h.deletedAt IS NULL
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:countryId IS NULL OR co.id = :countryId)
              AND (:minPrice IS NULL OR h.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR h.basePrice <= :maxPrice)
              AND (:minStarRating IS NULL OR h.starRating >= :minStarRating)
              AND (:search IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(co.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
              AND (:amenityCount = 0 OR EXISTS (
                    SELECT 1 FROM HotelAmenity ha WHERE ha.hotelId = h.id AND ha.amenityId IN :amenityIds))
            """;

    String PUBLIC_COUNT_SELECT = """
            SELECT COUNT(h) FROM Hotel h
            JOIN City c ON c.id = h.cityId
            JOIN Country co ON co.id = c.countryId
            WHERE h.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND h.deletedAt IS NULL
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:countryId IS NULL OR co.id = :countryId)
              AND (:minPrice IS NULL OR h.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR h.basePrice <= :maxPrice)
              AND (:minStarRating IS NULL OR h.starRating >= :minStarRating)
              AND (:search IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(co.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
              AND (:amenityCount = 0 OR EXISTS (
                    SELECT 1 FROM HotelAmenity ha WHERE ha.hotelId = h.id AND ha.amenityId IN :amenityIds))
            """;

    @Query(value = PUBLIC_LIST_SELECT, countQuery = PUBLIC_COUNT_SELECT)
    Page<HotelSummaryResponse> searchPublic(
            @Param("cityId") UUID cityId,
            @Param("countryId") UUID countryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minStarRating") Short minStarRating,
            @Param("search") String search,
            @Param("amenityIds") List<UUID> amenityIds,
            @Param("amenityCount") long amenityCount,
            Pageable pageable);


    /**
     * Slugs and last-modified dates for the sitemap.
     *
     * <p>Ordered by {@code updatedAt} descending so that if the sitemap ever has
     * to be truncated, the most recently changed pages are the ones that survive.
     */
    @Query("""
            SELECT new com.tourpackage.api.dto.response.SitemapEntry(h.slug, h.updatedAt)
              FROM Hotel h
             WHERE h.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND h.deletedAt IS NULL
             ORDER BY h.updatedAt DESC
            """)
    List<com.tourpackage.api.dto.response.SitemapEntry> findSitemapEntries();

}
