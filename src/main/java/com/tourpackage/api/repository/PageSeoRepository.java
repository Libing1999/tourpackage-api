package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.PageSeo;

public interface PageSeoRepository extends JpaRepository<PageSeo, UUID> {

    Optional<PageSeo> findByPath(String path);

    List<PageSeo> findAllByOrderByPathAsc();

    boolean existsByPath(String path);

    boolean existsByPathAndIdNot(String path, UUID id);

}
