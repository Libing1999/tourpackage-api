package com.tourpackage.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.Country;

public interface CountryRepository extends JpaRepository<Country, UUID> {
}
