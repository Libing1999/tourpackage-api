package com.tourpackage.api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.dto.response.InquiryAdminListResponse;
import com.tourpackage.api.entity.Inquiry;
import com.tourpackage.api.entity.InquiryStatus;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    long countByStatus(InquiryStatus status);

    // :search is CAST to string for the same reason as everywhere else in this
    // codebase — a null bind otherwise gets inferred as bytea by PostgreSQL and
    // the whole query fails on lower(bytea). See HotelRepository.
    String ADMIN_LIST_SELECT = """
            SELECT new com.tourpackage.api.dto.response.InquiryAdminListResponse(
                i.id, i.name, i.email, i.phone, i.travelDate, i.partySize,
                i.message, i.status, tp.title, i.createdAt)
            FROM Inquiry i
            LEFT JOIN TourPackage tp ON tp.id = i.packageId
            WHERE (:status IS NULL OR i.status = :status)
              AND (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(i.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    String ADMIN_COUNT_SELECT = """
            SELECT COUNT(i) FROM Inquiry i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:search IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(i.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    @Query(value = ADMIN_LIST_SELECT, countQuery = ADMIN_COUNT_SELECT)
    Page<InquiryAdminListResponse> searchAdmin(
            @Param("status") InquiryStatus status,
            @Param("search") String search,
            Pageable pageable);

}
