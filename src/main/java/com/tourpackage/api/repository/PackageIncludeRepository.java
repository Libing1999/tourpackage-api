package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.PackageInclude;

public interface PackageIncludeRepository extends JpaRepository<PackageInclude, UUID> {

    List<PackageInclude> findByPackageIdOrderByDisplayOrderAsc(UUID packageId);

    Optional<PackageInclude> findByIdAndPackageId(UUID id, UUID packageId);

}
