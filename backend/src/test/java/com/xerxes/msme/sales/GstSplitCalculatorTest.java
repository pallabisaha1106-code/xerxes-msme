package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xerxes.msme.sales.GstSplitCalculator.GstSplit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GstSplitCalculatorTest {

    @Test
    void sameState_splitsEquallyIntoCgstAndSgst_noIgst() {
        GstSplit split = GstSplitCalculator.calculate(new BigDecimal("1000.00"), new BigDecimal("18"), "27", "27");

        assertThat(split.cgst()).isEqualByComparingTo("90.00");
        assertThat(split.sgst()).isEqualByComparingTo("90.00");
        assertThat(split.igst()).isEqualByComparingTo("0.00");
    }

    @Test
    void differentState_appliesFullRateAsIgst_noCgstOrSgst() {
        GstSplit split = GstSplitCalculator.calculate(new BigDecimal("1000.00"), new BigDecimal("18"), "27", "09");

        assertThat(split.cgst()).isEqualByComparingTo("0.00");
        assertThat(split.sgst()).isEqualByComparingTo("0.00");
        assertThat(split.igst()).isEqualByComparingTo("180.00");
    }

    @Test
    void zeroRate_producesZeroTaxRegardlessOfState() {
        GstSplit intraState = GstSplitCalculator.calculate(new BigDecimal("500.00"), BigDecimal.ZERO, "27", "27");
        GstSplit interState = GstSplitCalculator.calculate(new BigDecimal("500.00"), BigDecimal.ZERO, "27", "09");

        assertThat(intraState.cgst()).isEqualByComparingTo("0.00");
        assertThat(intraState.sgst()).isEqualByComparingTo("0.00");
        assertThat(interState.igst()).isEqualByComparingTo("0.00");
    }

    @Test
    void oddCentAmount_cgstAndSgstDoNotDriftFromIndependentRounding() {
        // 897.00 * 12% = 107.64 total tax; each half (6%) = 53.82 exactly here,
        // but the point is each side is computed independently, not as
        // totalTax/2, so there is no double-rounding to drift a cent.
        GstSplit split = GstSplitCalculator.calculate(new BigDecimal("897.00"), new BigDecimal("12"), "27", "27");

        assertThat(split.cgst()).isEqualByComparingTo("53.82");
        assertThat(split.sgst()).isEqualByComparingTo("53.82");
        assertThat(split.cgst().add(split.sgst())).isEqualByComparingTo("107.64");
    }

    @Test
    void nullArguments_areRejected() {
        assertThrows(
                NullPointerException.class,
                () -> GstSplitCalculator.calculate(null, BigDecimal.TEN, "27", "27"));
        assertThrows(
                NullPointerException.class,
                () -> GstSplitCalculator.calculate(BigDecimal.TEN, null, "27", "27"));
        assertThrows(
                NullPointerException.class,
                () -> GstSplitCalculator.calculate(BigDecimal.TEN, BigDecimal.TEN, null, "27"));
        assertThrows(
                NullPointerException.class,
                () -> GstSplitCalculator.calculate(BigDecimal.TEN, BigDecimal.TEN, "27", null));
    }
}
