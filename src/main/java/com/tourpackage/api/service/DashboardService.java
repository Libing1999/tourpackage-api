package com.tourpackage.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.BookingAdminListResponse;
import com.tourpackage.api.dto.response.DashboardStatsResponse;
import com.tourpackage.api.dto.response.DashboardStatsResponse.Counts;
import com.tourpackage.api.dto.response.DashboardStatsResponse.MonthlyRevenuePoint;
import com.tourpackage.api.dto.response.DashboardStatsResponse.RevenueCards;
import com.tourpackage.api.dto.response.DashboardStatsResponse.StatusSlice;
import com.tourpackage.api.dto.response.DashboardStatsResponse.TopSeller;
import com.tourpackage.api.entity.BookingStatus;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.InquiryStatus;
import com.tourpackage.api.repository.BookingRepository;
import com.tourpackage.api.repository.HotelRepository;
import com.tourpackage.api.repository.InquiryRepository;
import com.tourpackage.api.repository.NewsletterSubscriberRepository;
import com.tourpackage.api.repository.TourPackageRepository;
import com.tourpackage.api.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** How much history the revenue chart shows. */
    private static final int CHART_MONTHS = 12;
    private static final int TOP_SELLERS = 5;
    private static final int LATEST_BOOKINGS = 8;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;
    private final HotelRepository hotelRepository;
    private final TourPackageRepository tourPackageRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;

    public DashboardService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            InquiryRepository inquiryRepository,
            HotelRepository hotelRepository,
            TourPackageRepository tourPackageRepository,
            NewsletterSubscriberRepository newsletterSubscriberRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.inquiryRepository = inquiryRepository;
        this.hotelRepository = hotelRepository;
        this.tourPackageRepository = tourPackageRepository;
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
    }

    public DashboardStatsResponse getStats() {
        YearMonth thisMonth = YearMonth.now(ZoneOffset.UTC);
        Instant thisMonthStart = startOf(thisMonth);
        Instant lastMonthStart = startOf(thisMonth.minusMonths(1));
        Instant chartStart = startOf(thisMonth.minusMonths(CHART_MONTHS - 1L));

        BigDecimal total = bookingRepository.sumRevenue();
        BigDecimal thisMonthRevenue = bookingRepository.sumRevenueFrom(thisMonthStart);
        BigDecimal lastMonthRevenue = bookingRepository.sumRevenueBetween(lastMonthStart, thisMonthStart);

        return new DashboardStatsResponse(
                new RevenueCards(
                        total,
                        thisMonthRevenue,
                        lastMonthRevenue,
                        percentChange(lastMonthRevenue, thisMonthRevenue),
                        // Every price in the system is stored per-booking with
                        // its own currency; the dashboard reports one figure, so
                        // it labels it with the currency the catalogue is priced
                        // in. Mixed-currency reporting would need FX rates and a
                        // reporting currency setting, which don't exist yet.
                        "USD"),
                buildCounts(),
                buildRevenueByMonth(chartStart, thisMonth),
                buildBookingsByStatus(),
                buildTopSellers(),
                bookingRepository.searchAdmin(null, null,
                        PageRequest.of(0, LATEST_BOOKINGS, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .getContent());
    }

    private Counts buildCounts() {
        return new Counts(
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                userRepository.count(),
                inquiryRepository.countByStatus(InquiryStatus.NEW),
                hotelRepository.countByStatusAndDeletedAtIsNull(ContentStatus.PUBLISHED),
                tourPackageRepository.countByStatusAndDeletedAtIsNull(ContentStatus.PUBLISHED),
                newsletterSubscriberRepository.countByActiveTrue());
    }

    /**
     * Months with no bookings still need a point, or the chart would draw a
     * line straight from one busy month to the next and imply activity that
     * didn't happen. The query only returns months that have rows, so the
     * gaps are filled here.
     */
    private List<MonthlyRevenuePoint> buildRevenueByMonth(Instant chartStart, YearMonth thisMonth) {
        List<Object[]> rows = bookingRepository.revenueByMonth(chartStart);

        List<MonthlyRevenuePoint> points = new ArrayList<>();
        for (int i = CHART_MONTHS - 1; i >= 0; i--) {
            String month = thisMonth.minusMonths(i).toString();

            Object[] match = rows.stream()
                    .filter(row -> month.equals(row[0]))
                    .findFirst()
                    .orElse(null);

            points.add(match == null
                    ? new MonthlyRevenuePoint(month, BigDecimal.ZERO, 0L)
                    : new MonthlyRevenuePoint(month, (BigDecimal) match[1], (Long) match[2]));
        }
        return points;
    }

    private List<StatusSlice> buildBookingsByStatus() {
        List<Object[]> rows = bookingRepository.countGroupedByStatus();

        // Every status appears even at zero, so the legend doesn't reshuffle
        // as bookings move between them.
        return java.util.Arrays.stream(BookingStatus.values())
                .map(status -> new StatusSlice(
                        status.name(),
                        rows.stream()
                                .filter(row -> row[0] == status)
                                .map(row -> (Long) row[1])
                                .findFirst()
                                .orElse(0L)))
                .toList();
    }

    private List<TopSeller> buildTopSellers() {
        PageRequest limit = PageRequest.of(0, TOP_SELLERS);

        List<TopSeller> sellers = new ArrayList<>();
        bookingRepository.topHotelsByRevenue(limit)
                .forEach(row -> sellers.add(new TopSeller(
                        (String) row[0], "HOTEL", (Long) row[1], (BigDecimal) row[2])));
        bookingRepository.topPackagesByRevenue(limit)
                .forEach(row -> sellers.add(new TopSeller(
                        (String) row[0], "PACKAGE", (Long) row[1], (BigDecimal) row[2])));

        // Merged from two queries, so the combined list has to be re-ranked.
        return sellers.stream()
                .sorted(Comparator.comparing(TopSeller::revenue).reversed())
                .limit(TOP_SELLERS)
                .toList();
    }

    private static Instant startOf(YearMonth month) {
        return month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Null rather than a made-up number when the base is zero — there's no
     * honest percentage increase from nothing. */
    private static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
        if (from == null || from.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return to.subtract(from)
                .multiply(BigDecimal.valueOf(100))
                .divide(from, 1, RoundingMode.HALF_UP);
    }

}
