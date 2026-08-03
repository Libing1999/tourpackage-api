package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.NavGroup;
import com.tourpackage.api.entity.NavLink;

public interface NavLinkRepository extends JpaRepository<NavLink, UUID> {

    List<NavLink> findByActiveTrueOrderByNavGroupAscDisplayOrderAsc();

    List<NavLink> findAllByOrderByNavGroupAscDisplayOrderAsc();

    List<NavLink> findByNavGroupAndActiveTrueOrderByDisplayOrderAsc(NavGroup navGroup);

}
