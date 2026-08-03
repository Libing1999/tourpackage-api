package com.tourpackage.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.dto.response.CustomerResponse;
import com.tourpackage.api.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Email is stored lowercase (normalized on write), so an exact match is
    // enough — no need for a case-insensitive comparison here.
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /**
     * Customers with their booking activity rolled up. The LEFT JOIN keeps
     * customers who haven't booked yet — they're still customers, and a list
     * that silently hid them would misreport the total.
     *
     * <p>Only CONFIRMED/COMPLETED bookings count toward spend, matching how
     * the dashboard reports revenue.
     */
    @Query("""
            SELECT new com.tourpackage.api.dto.response.CustomerResponse(
                u.id, CONCAT(u.firstName, ' ', u.lastName), u.email, u.phone,
                COUNT(b),
                COALESCE(SUM(CASE WHEN b.status IN (com.tourpackage.api.entity.BookingStatus.CONFIRMED,
                                                    com.tourpackage.api.entity.BookingStatus.COMPLETED)
                                  THEN b.totalAmount ELSE 0 END), 0),
                u.createdAt)
            FROM User u
            LEFT JOIN Booking b ON b.userId = u.id
            WHERE u.deletedAt IS NULL
              AND (:search IS NULL
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            GROUP BY u.id, u.firstName, u.lastName, u.email, u.phone, u.createdAt
            ORDER BY u.createdAt DESC
            """)
    Page<CustomerResponse> searchCustomers(@Param("search") String search, Pageable pageable);

}
