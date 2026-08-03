package com.tourpackage.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tourpackage.api.entity.TourPackage;

/**
 * The one place package pricing is decided. Both the public package detail
 * response (which shows the per-person rates) and {@code BookingService}
 * (which charges them) go through here, so a quoted price and a booked price
 * can't drift apart.
 *
 * <p>The child rate is a single configurable percentage rather than a column
 * because it's currently a uniform business rule, not per-package data. If
 * packages ever need their own child policies, this becomes a
 * {@code child_price_percent} column on {@code tour_packages} and this class
 * reads it from the entity instead.
 */
@Component
public class PackagePricing {

    private final BigDecimal childRate;

    public PackagePricing(@Value("${app.booking.child-price-percent:70}") int childPricePercent) {
        this.childRate = BigDecimal.valueOf(childPricePercent).divide(BigDecimal.valueOf(100));
    }

    /** The discounted price when the package is on offer, otherwise list price. */
    public BigDecimal pricePerAdult(TourPackage tourPackage) {
        return tourPackage.getDiscountPrice() != null ? tourPackage.getDiscountPrice() : tourPackage.getPrice();
    }

    public BigDecimal pricePerChild(TourPackage tourPackage) {
        return pricePerAdult(tourPackage).multiply(childRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal total(TourPackage tourPackage, short adults, short children) {
        return pricePerAdult(tourPackage).multiply(BigDecimal.valueOf(adults))
                .add(pricePerChild(tourPackage).multiply(BigDecimal.valueOf(children)))
                .setScale(2, RoundingMode.HALF_UP);
    }

}
