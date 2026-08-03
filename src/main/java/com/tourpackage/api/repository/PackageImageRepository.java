package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.entity.PackageImage;

public interface PackageImageRepository extends JpaRepository<PackageImage, UUID> {

    List<PackageImage> findByPackageIdOrderByDisplayOrderAsc(UUID packageId);

    Optional<PackageImage> findByIdAndPackageId(UUID id, UUID packageId);

    // One is_cover=true row per package (partial unique index) — call before
    // saving any row with isCover=true. Same pattern as HotelImageRepository.
    @Modifying
    @Query("UPDATE PackageImage pi SET pi.cover = false WHERE pi.packageId = :packageId AND pi.cover = true")
    void clearCoverForPackage(@Param("packageId") UUID packageId);

}
