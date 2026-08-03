package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.ContentBlock;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, UUID> {

    List<ContentBlock> findByActiveTrue();

    Optional<ContentBlock> findByKey(String key);

    boolean existsByKey(String key);

    boolean existsByKeyAndIdNot(String key, UUID id);

}
