package com.xerxes.msme.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * CGST+SGST applies within a state, IGST applies across states - determined
 * by comparing the supplier's (tenant's) GST state code to the place of
 * supply's state code. Pure function so the many GST edge cases (0% rate,
 * intra vs inter-state, rounding) can be unit tested without a database.
 */
public final class GstSplitCalculator {

    private GstSplitCalculator() {}

    public record GstSplit(BigDecimal cgst, BigDecimal sgst, BigDecimal igst) {}

    public static GstSplit calculate(
            BigDecimal taxableAmount, BigDecimal gstRate, String supplierStateCode, String placeOfSupplyStateCode) {
        Objects.requireNonNull(taxableAmount, "taxableAmount must not be null");
        Objects.requireNonNull(gstRate, "gstRate must not be null");
        Objects.requireNonNull(supplierStateCode, "supplierStateCode must not be null");
        Objects.requireNonNull(placeOfSupplyStateCode, "placeOfSupplyStateCode must not be null");

        // CGST and SGST are each independently half the rate, not "total tax / 2" -
        // computing them from a shared totalTax would round twice and could drift
        // a cent off the true total on odd-cent amounts.
        if (supplierStateCode.equals(placeOfSupplyStateCode)) {
            BigDecimal cgst = taxableAmount
                    .multiply(gstRate)
                    .divide(BigDecimal.valueOf(200), 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            return new GstSplit(cgst, cgst, BigDecimal.ZERO.setScale(2));
        }

        BigDecimal igst = taxableAmount
                .multiply(gstRate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        return new GstSplit(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), igst);
    }
}
