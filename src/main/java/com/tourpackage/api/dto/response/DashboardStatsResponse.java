package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the dashboard home renders, in one response. The page shows all
 * of it at once, so splitting it across six endpoints would mean six round
 * trips and six loading states for a screen that's only useful complete.
 */
public record DashboardStatsResponse(
        RevenueCards revenue,
        Counts counts,
        List<MonthlyRevenuePoint> revenueByMonth,
        List<StatusSlice> bookingsByStatus,
        List<TopSeller> topSellers,
        List<BookingAdminListResponse> latestBookings
) {

    /**
     * Revenue counts CONFIRMED and COMPLETED bookings only — PENDING is a
     * request nobody has agreed to yet, and counting it would inflate the
     * headline number with bookings that may never be honoured.
     */
    public record RevenueCards(
            BigDecimal total,
            BigDecimal thisMonth,
            BigDecimal lastMonth,
            /** Percent change month over month; null when last month was zero,
             * since "up from nothing" has no meaningful percentage. */
            BigDecimal changePercent,
            String currencyCode
    ) {
    }

    public record Counts(
            long totalBookings,
            long pendingBookings,
            long totalCustomers,
            long newInquiries,
            long publishedHotels,
            long publishedPackages,
            long newsletterSubscribers
    ) {
    }

    /** {@code month} is an ISO year-month ("2026-08") so the client can format
     * it for its own locale rather than parsing a pre-formatted label. */
    public record MonthlyRevenuePoint(String month, BigDecimal revenue, long bookings) {
    }

    public record StatusSlice(String status, long count) {
    }

    public record TopSeller(String name, String type, long bookings, BigDecimal revenue) {
    }

}
