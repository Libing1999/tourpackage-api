package com.tourpackage.api.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.dto.response.BookingAdminListResponse;
import com.tourpackage.api.entity.Booking;
import com.tourpackage.api.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByStatus(BookingStatus status);

    // Revenue counts only bookings someone has actually agreed to — see
    // DashboardStatsResponse.RevenueCards.
    //
    // Three methods rather than one with nullable bounds: PostgreSQL can't
    // infer a type for a parameter that only ever appears in `:p IS NULL OR
    // …`, and binding both as null failed outright with "could not determine
    // data type of parameter $1". Casting each one would work, but not needing
    // the cast is better — every call site here knows its own bounds.
    String CONFIRMED_REVENUE = """
            SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
            WHERE b.status IN (com.tourpackage.api.entity.BookingStatus.CONFIRMED,
                               com.tourpackage.api.entity.BookingStatus.COMPLETED)
            """;

    @Query(CONFIRMED_REVENUE)
    BigDecimal sumRevenue();

    @Query(CONFIRMED_REVENUE + " AND b.createdAt >= :from")
    BigDecimal sumRevenueFrom(@Param("from") Instant from);

    @Query(CONFIRMED_REVENUE + " AND b.createdAt >= :from AND b.createdAt < :to")
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT b.status, COUNT(b) FROM Booking b
            GROUP BY b.status
            """)
    List<Object[]> countGroupedByStatus();

    /**
     * Revenue and booking count per calendar month. Grouped in the database
     * rather than by loading every booking and bucketing in Java — the result
     * is a dozen rows either way, but only one of those approaches stays that
     * way as the table grows.
     */
    @Query("""
            SELECT FUNCTION('to_char', b.createdAt, 'YYYY-MM'),
                   COALESCE(SUM(CASE WHEN b.status IN (com.tourpackage.api.entity.BookingStatus.CONFIRMED,
                                                       com.tourpackage.api.entity.BookingStatus.COMPLETED)
                                     THEN b.totalAmount ELSE 0 END), 0),
                   COUNT(b)
            FROM Booking b
            WHERE b.createdAt >= :from
            GROUP BY FUNCTION('to_char', b.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('to_char', b.createdAt, 'YYYY-MM')
            """)
    List<Object[]> revenueByMonth(@Param("from") Instant from);

    /** Best-selling hotels and packages by confirmed revenue. Two queries
     * rather than one union, since the joins differ per type. */
    @Query("""
            SELECT h.name, COUNT(b), COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            JOIN HotelRoom hr ON hr.id = b.hotelRoomId
            JOIN Hotel h ON h.id = hr.hotelId
            WHERE b.status IN (com.tourpackage.api.entity.BookingStatus.CONFIRMED,
                               com.tourpackage.api.entity.BookingStatus.COMPLETED)
            GROUP BY h.name
            ORDER BY SUM(b.totalAmount) DESC
            """)
    List<Object[]> topHotelsByRevenue(Pageable pageable);

    @Query("""
            SELECT tp.title, COUNT(b), COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            JOIN TourPackage tp ON tp.id = b.packageId
            WHERE b.status IN (com.tourpackage.api.entity.BookingStatus.CONFIRMED,
                               com.tourpackage.api.entity.BookingStatus.COMPLETED)
            GROUP BY tp.title
            ORDER BY SUM(b.totalAmount) DESC
            """)
    List<Object[]> topPackagesByRevenue(Pageable pageable);

    /**
     * How many rooms of this type are already committed for any night in the
     * requested range. Two stays overlap when each starts before the other
     * ends — checkout day itself is free, so the comparison is strict on both
     * sides rather than inclusive.
     *
     * <p>CANCELLED bookings release their inventory; PENDING ones still hold it,
     * since an unpaid booking that hasn't been rejected yet is still a claim on
     * the room.
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.hotelRoomId = :hotelRoomId
              AND b.status IN (com.tourpackage.api.entity.BookingStatus.PENDING,
                               com.tourpackage.api.entity.BookingStatus.CONFIRMED)
              AND b.travelDate < :checkOutDate
              AND b.returnDate > :checkInDate
            """)
    long countOverlapping(
            @Param("hotelRoomId") UUID hotelRoomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);

    // COALESCE so a package booking shows the package title where a hotel
    // booking shows the hotel name — the admin table has one "what was booked"
    // column and both types have to fill it.
    String ADMIN_LIST_SELECT = """
            SELECT new com.tourpackage.api.dto.response.BookingAdminListResponse(
                b.id, b.bookingNumber, b.bookingType, b.status,
                COALESCE(h.name, tp.title), hr.name,
                CONCAT(u.firstName, ' ', u.lastName), u.email,
                b.travelDate, b.returnDate, b.totalAmount, b.currencyCode, b.createdAt)
            FROM Booking b
            JOIN User u ON u.id = b.userId
            LEFT JOIN HotelRoom hr ON hr.id = b.hotelRoomId
            LEFT JOIN Hotel h ON h.id = hr.hotelId
            LEFT JOIN TourPackage tp ON tp.id = b.packageId
            WHERE (:status IS NULL OR b.status = :status)
              AND (:search IS NULL
                   OR LOWER(b.bookingNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    String ADMIN_COUNT_SELECT = """
            SELECT COUNT(b) FROM Booking b
            JOIN User u ON u.id = b.userId
            WHERE (:status IS NULL OR b.status = :status)
              AND (:search IS NULL
                   OR LOWER(b.bookingNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """;

    @Query(value = ADMIN_LIST_SELECT, countQuery = ADMIN_COUNT_SELECT)
    Page<BookingAdminListResponse> searchAdmin(
            @Param("status") BookingStatus status,
            @Param("search") String search,
            Pageable pageable);

}
