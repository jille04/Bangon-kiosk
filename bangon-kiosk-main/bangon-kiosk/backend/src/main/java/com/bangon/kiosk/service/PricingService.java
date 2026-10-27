package com.bangon.kiosk.service;

import com.bangon.kiosk.dto.ApiDtos.Choices;
import com.bangon.kiosk.entity.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/** Computes prices, nutrition estimates and loyalty values on the server. */
@Service
public class PricingService {

    // Customization options
    private static final Set<String> SIZES = Set.of("small", "medium", "large");
    private static final Set<String> MILKS = Set.of("whole", "oat", "almond");
    private static final Set<Integer> SWEETNESS_LEVELS = Set.of(25, 50, 100);

    // Price adjustments in pesos
    private static final double SMALL_SIZE_DISCOUNT = 15;
    private static final double LARGE_SIZE_SURCHARGE = 25;
    private static final double ALT_MILK_SURCHARGE = 20;
    private static final double EXTRA_SHOT_PRICE = 30;
    private static final double CARAMEL_PRICE = 20;
    private static final double CREAM_PRICE = 20;

    // Nutrition adjustments
    private static final double SMALL_SIZE_FACTOR = 0.85;
    private static final double LARGE_SIZE_FACTOR = 1.2;
    private static final int OAT_MILK_CALORIES = 20;
    private static final int ALMOND_MILK_CALORIES = 10;
    private static final int EXTRA_SHOT_CAFFEINE = 65;
    private static final int EXTRA_SHOT_CALORIES = 5;
    private static final int CARAMEL_CALORIES = 60;
    private static final int CREAM_CALORIES = 80;

    // Loyalty program
    private static final int PESOS_PER_POINT = 50;
    private static final int POINTS_TO_REDEEM = 100;
    private static final BigDecimal REDEMPTION_DISCOUNT = BigDecimal.valueOf(20);

    /** The priced and normalized result of a product with its customizations. */
    public record Quote(BigDecimal price, int caffeine, int calories, String size, String milk,
                        int sweetness, boolean extraShot, boolean caramel, boolean cream) {
    }

    /** Calculates the price and nutrition for a product with the given choices. */
    public Quote quote(Product p, Choices c) {
        if (p == null || !p.isActive()) {
            throw new IllegalArgumentException("Unavailable product");
        }

        // Pastries and desserts cannot be customized.
        if (p.isPastry()) {
            boolean customized = c != null
                    && (c.size() != null || c.milk() != null || c.sweetness() != null
                    || c.extraShot() || c.caramel() || c.cream());
            if (customized) {
                throw new IllegalArgumentException("Pastries cannot be customized");
            }
            return new Quote(p.getPrice(), p.getCaffeine(), p.getCalories(), null, null, 0, false, false, false);
        }

        // Fall back to defaults when a choice is not provided.
        String size = (c == null || c.size() == null) ? "medium" : c.size();
        String milk = (c == null || c.milk() == null) ? "whole" : c.milk();
        int sweet = (c == null || c.sweetness() == null) ? 100 : c.sweetness();

        if (!SIZES.contains(size) || !MILKS.contains(milk) || !SWEETNESS_LEVELS.contains(sweet)) {
            throw new IllegalArgumentException("Invalid customization");
        }

        boolean extra = c != null && c.extraShot();
        boolean caramel = c != null && c.caramel();
        boolean cream = c != null && c.cream();

        double price = p.getPrice().doubleValue();
        double caffeine = p.getCaffeine();
        double calories = p.getCalories();

        if (size.equals("small")) {
            price -= SMALL_SIZE_DISCOUNT;
            caffeine *= SMALL_SIZE_FACTOR;
            calories *= SMALL_SIZE_FACTOR;
        }
        if (size.equals("large")) {
            price += LARGE_SIZE_SURCHARGE;
            caffeine *= LARGE_SIZE_FACTOR;
            calories *= LARGE_SIZE_FACTOR;
        }

        if (!milk.equals("whole")) {
            price += ALT_MILK_SURCHARGE;
        }
        if (milk.equals("oat")) {
            calories += OAT_MILK_CALORIES;
        }
        if (milk.equals("almond")) {
            calories += ALMOND_MILK_CALORIES;
        }

        // Less sweetness means fewer calories.
        calories *= 0.72 + 0.28 * (sweet / 100.0);

        if (extra) {
            price += EXTRA_SHOT_PRICE;
            caffeine += EXTRA_SHOT_CAFFEINE;
            calories += EXTRA_SHOT_CALORIES;
        }
        if (caramel) {
            price += CARAMEL_PRICE;
            calories += CARAMEL_CALORIES;
        }
        if (cream) {
            price += CREAM_PRICE;
            calories += CREAM_CALORIES;
        }

        return new Quote(
                BigDecimal.valueOf(Math.round(price)).setScale(2),
                (int) Math.round(caffeine),
                (int) Math.round(calories),
                size, milk, sweet, extra, caramel, cream);
    }

    /** Loyalty points earned for a paid amount: one point per 50 pesos, rounded down. */
    public int earnedPoints(BigDecimal paid) {
        return paid.divide(BigDecimal.valueOf(PESOS_PER_POINT), 0, RoundingMode.DOWN).intValue();
    }

    /** Discount applied when redeeming points; requires at least 100 points. */
    public BigDecimal discount(boolean redeem, int points) {
        if (redeem && points < POINTS_TO_REDEEM) {
            throw new IllegalArgumentException("Insufficient loyalty points");
        }
        return redeem ? REDEMPTION_DISCOUNT : BigDecimal.ZERO;
    }
}
