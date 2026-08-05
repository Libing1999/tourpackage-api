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

import com.tourpackage.api.dto.response.TourPackageAdminListResponse;
import com.tourpackage.api.dto.response.TourPackageSummaryResponse;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.DifficultyLevel;
import com.tourpackage.api.entity.TourPackage;

public interface TourPackageRepository extends JpaRepository<TourPackage, UUID> {

    String SUMMARY_SELECT = """
            SELECT new com.tourpackage.api.dto.response.TourPackageSummaryResponse(
                tp.id, tp.title, tp.slug, c.name, co.name, pi.url,
                tp.durationDays, tp.durationNights, tp.price, tp.discountPrice,
                tp.ratingAverage, tp.ratingCount)
            FROM TourPackage tp
            JOIN City c ON c.id = tp.cityId
            JOIN Country co ON co.id = tp.countryId
            LEFT JOIN PackageImage pi ON pi.packageId = tp.id AND pi.cover = true
            WHERE tp.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND tp.deletedAt IS NULL
            """;

    @Query(SUMMARY_SELECT + " ORDER BY tp.featured DESC, tp.ratingAverage DESC, tp.ratingCount DESC")
    List<TourPackageSummaryResponse> findBestPackages(Pageable pageable);

    @Query(SUMMARY_SELECT + " AND tp.discountPrice IS NOT NULL "
            + "ORDER BY ((tp.price - tp.discountPrice) / tp.price) DESC")
    List<TourPackageSummaryResponse> findSpecialOffers(Pageable pageable);

    @Query("""
            SELECT tp.cityId, COUNT(tp)
            FROM TourPackage tp
            WHERE tp.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND tp.deletedAt IS NULL
            GROUP BY tp.cityId
            """)
    List<Object[]> countPublishedByCity();

    long countByStatusAndDeletedAtIsNull(ContentStatus status);

    Optional<TourPackage> findByIdAndDeletedAtIsNull(UUID id);

    Optional<TourPackage> findBySlugAndDeletedAtIsNullAndStatus(String slug, ContentStatus status);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndIdNotAndDeletedAtIsNull(String slug, UUID id);

    // :search is CAST to string explicitly — binding it null otherwise leaves
    // PostgreSQL to infer a type for the parameter, and it picks bytea, so the
    // whole query fails with "function lower(bytea) does not exist" even though
    // the IS NULL guard would have short-circuited it. Same fix as HotelRepository.
    String ADMIN_LIST_SELECT = """
            SELECT new com.tourpackage.api.dto.response.TourPackageAdminListResponse(
                tp.id, tp.title, tp.slug, c.name, co.name, pi.url,
                tp.durationDays, tp.durationNights, tp.price, tp.discountPrice, tp.currencyCode,
                tp.difficultyLevel, tp.ratingAverage, tp.ratingCount, tp.featured, tp.status)
            FROM TourPackage tp
            JOIN City c ON c.id = tp.cityId
            JOIN Country co ON co.id = tp.countryId
            LEFT JOIN PackageImage pi ON pi.packageId = tp.id AND pi.cover = true
            WHERE tp.deletedAt IS NULL
              AND (:status IS NULL OR tp.status = :status)
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:search IS NULL OR LOWER(tp.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    String ADMIN_COUNT_SELECT = """
            SELECT COUNT(tp) FROM TourPackage tp
            JOIN City c ON c.id = tp.cityId
            WHERE tp.deletedAt IS NULL
              AND (:status IS NULL OR tp.status = :status)
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:search IS NULL OR LOWER(tp.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    @Query(value = ADMIN_LIST_SELECT, countQuery = ADMIN_COUNT_SELECT)
    Page<TourPackageAdminListResponse> searchAdmin(
            @Param("status") ContentStatus status,
            @Param("cityId") UUID cityId,
            @Param("search") String search,
            Pageable pageable);

    String PUBLIC_FILTERS = """
            WHERE tp.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND tp.deletedAt IS NULL
              AND (:cityId IS NULL OR c.id = :cityId)
              AND (:countryId IS NULL OR co.id = :countryId)
              AND (:minPrice IS NULL OR COALESCE(tp.discountPrice, tp.price) >= :minPrice)
              AND (:maxPrice IS NULL OR COALESCE(tp.discountPrice, tp.price) <= :maxPrice)
              AND (:minDurationDays IS NULL OR tp.durationDays >= :minDurationDays)
              AND (:maxDurationDays IS NULL OR tp.durationDays <= :maxDurationDays)
              AND (:difficultyLevel IS NULL OR tp.difficultyLevel = :difficultyLevel)
              AND (:discountedOnly = false OR tp.discountPrice IS NOT NULL)
              AND (:search IS NULL OR LOWER(tp.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(co.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    String PUBLIC_LIST_SELECT = """
            SELECT new com.tourpackage.api.dto.response.TourPackageSummaryResponse(
                tp.id, tp.title, tp.slug, c.name, co.name, pi.url,
                tp.durationDays, tp.durationNights, tp.price, tp.discountPrice,
                tp.ratingAverage, tp.ratingCount)
            FROM TourPackage tp
            JOIN City c ON c.id = tp.cityId
            JOIN Country co ON co.id = tp.countryId
            LEFT JOIN PackageImage pi ON pi.packageId = tp.id AND pi.cover = true
            """ + PUBLIC_FILTERS;

    String PUBLIC_COUNT_SELECT = """
            SELECT COUNT(tp) FROM TourPackage tp
            JOIN City c ON c.id = tp.cityId
            JOIN Country co ON co.id = tp.countryId
            """ + PUBLIC_FILTERS;

    @Query(value = PUBLIC_LIST_SELECT, countQuery = PUBLIC_COUNT_SELECT)
    Page<TourPackageSummaryResponse> searchPublic(
            @Param("cityId") UUID cityId,
            @Param("countryId") UUID countryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minDurationDays") Short minDurationDays,
            @Param("maxDurationDays") Short maxDurationDays,
            @Param("difficultyLevel") DifficultyLevel difficultyLevel,
            @Param("discountedOnly") boolean discountedOnly,
            @Param("search") String search,
            Pageable pageable);


    /**
     * Slugs and last-modified dates for the sitemap.
     *
     * <p>Ordered by {@code updatedAt} descending so that if the sitemap ever has
     * to be truncated, the most recently changed pages are the ones that survive.
     */
    @Query("""
            SELECT new com.tourpackage.api.dto.response.SitemapEntry(p.slug, p.updatedAt)
              FROM TourPackage p
             WHERE p.status = com.tourpackage.api.entity.ContentStatus.PUBLISHED AND p.deletedAt IS NULL
             ORDER BY p.updatedAt DESC
            """)
    List<com.tourpackage.api.dto.response.SitemapEntry> findSitemapEntries();

}
