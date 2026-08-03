package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.PackageExclude;

public interface PackageExcludeRepository extends JpaRepository<PackageExclude, UUID> {

    List<PackageExclude> findByPackageIdOrderByDisplayOrderAsc(UUID packageId);

    Optional<PackageExclude> findByIdAndPackageId(UUID id, UUID packageId);

}
