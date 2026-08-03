package com.tourpackage.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.entity.Banner;
import com.tourpackage.api.entity.BannerPlacement;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    /** Active and within its scheduling window, for one surface. */
    @Query("""
            SELECT b FROM Banner b
            WHERE b.active = true
              AND b.placement = :placement
              AND (b.startsAt IS NULL OR b.startsAt <= :now)
              AND (b.endsAt IS NULL OR b.endsAt >= :now)
            ORDER BY b.displayOrder ASC
            """)
    List<Banner> findActiveBanners(
            @Param("placement") BannerPlacement placement,
            @Param("now") Instant now);

    List<Banner> findAllByOrderByPlacementAscDisplayOrderAsc();

}
