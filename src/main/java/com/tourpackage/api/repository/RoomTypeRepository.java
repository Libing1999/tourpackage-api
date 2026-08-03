package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findAllByOrderByDisplayOrderAscNameAsc();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

}
