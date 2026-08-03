package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.GalleryImage;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {

    List<GalleryImage> findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    List<GalleryImage> findAllByOrderByDisplayOrderAscCreatedAtAsc();

}
