package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.Faq;

public interface FaqRepository extends JpaRepository<Faq, UUID> {

    List<Faq> findByActiveTrueOrderByCategoryAscDisplayOrderAsc();

}
