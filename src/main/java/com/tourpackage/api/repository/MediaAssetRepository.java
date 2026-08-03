package com.tourpackage.api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.MediaAsset;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    Page<MediaAsset> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MediaAsset> findByFolderOrderByCreatedAtDesc(String folder, Pageable pageable);

}
