package com.tourpackage.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tourpackage.api.entity.TourPackage;

class PackagePricingTest {

    private final PackagePricing pricing = new PackagePricing(70);

    private TourPackage pkg(String price, String discountPrice) {
        TourPackage p = new TourPackage();
        p.setPrice(new BigDecimal(price));
        p.setDiscountPrice(discountPrice == null ? null : new BigDecimal(discountPrice));
        return p;
    }

    @Test
    @DisplayName("adult pays list price when there is no offer")
    void adultPaysListPrice() {
        assertThat(pricing.pricePerAdult(pkg("1000.00", null))).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("adult pays the discounted price when one is set")
    void adultPaysDiscountedPrice() {
        assertThat(pricing.pricePerAdult(pkg("1000.00", "799.00"))).isEqualByComparingTo("799.00");
    }

    @Test
    @DisplayName("child rate applies to the discounted price, not the list price")
    void childRateAppliesToEffectivePrice() {
        // The whole point of resolving the adult price first: 70% of the list
        // price would overcharge every child on a discounted package.
        assertThat(pricing.pricePerChild(pkg("1000.00", "800.00"))).isEqualByComparingTo("560.00");
    }

    @Test
    @DisplayName("child price is rounded to cents, never left with trailing fractions")
    void childPriceRoundsToCents() {
        // 333.33 * 0.7 = 233.331 — a currency amount must not carry a third decimal.
        BigDecimal child = pricing.pricePerChild(pkg("333.33", null));
        assertThat(child.scale()).isEqualTo(2);
        assertThat(child).isEqualByComparingTo("233.33");
    }

    @Test
    @DisplayName("total combines adults and children")
    void totalCombinesTravellers() {
        // 2 x 1000 + 2 x 700
        assertThat(pricing.total(pkg("1000.00", null), (short) 2, (short) 2))
                .isEqualByComparingTo("3400.00");
    }

    @Test
    @DisplayName("a booking with no children costs only the adult fares")
    void totalWithoutChildren() {
        assertThat(pricing.total(pkg("1000.00", null), (short) 3, (short) 0))
                .isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("a different child percentage is honoured")
    void configurableChildPercentage() {
        assertThat(new PackagePricing(50).pricePerChild(pkg("1000.00", null)))
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("a free child rate produces no charge rather than an error")
    void zeroChildPercentage() {
        assertThat(new PackagePricing(0).total(pkg("1000.00", null), (short) 1, (short) 3))
                .isEqualByComparingTo("1000.00");
    }

}
