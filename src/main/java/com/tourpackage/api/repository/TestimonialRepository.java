package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tourpackage.api.dto.response.TestimonialResponse;
import com.tourpackage.api.entity.Testimonial;

public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    @Query("""
            SELECT new com.tourpackage.api.dto.response.TestimonialResponse(
                t.id, t.customerName, t.customerAvatarUrl, co.name, t.rating, t.message, tp.title)
            FROM Testimonial t
            LEFT JOIN Country co ON co.id = t.customerCountryId
            LEFT JOIN TourPackage tp ON tp.id = t.packageId
            WHERE t.featured = true AND t.active = true
            ORDER BY t.createdAt DESC
            """)
    List<TestimonialResponse> findFeaturedTestimonials(Pageable pageable);

}
