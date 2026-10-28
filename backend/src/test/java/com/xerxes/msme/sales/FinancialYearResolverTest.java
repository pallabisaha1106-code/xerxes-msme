package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FinancialYearResolverTest {

    @Test
    void dateInAprilToDecember_belongsToFyStartingSameYear() {
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2025, 4, 1))).isEqualTo("2025-26");
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2025, 12, 31))).isEqualTo("2025-26");
    }

    @Test
    void dateInJanuaryToMarch_belongsToFyStartingPreviousYear() {
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2026, 1, 1))).isEqualTo("2025-26");
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2026, 3, 31))).isEqualTo("2025-26");
    }

    @Test
    void fyRollsOverExactlyOnAprilFirst() {
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2026, 3, 31))).isEqualTo("2025-26");
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
    }

    @Test
    void centuryRollover_formatsShortEndYearWithLeadingZero() {
        assertThat(FinancialYearResolver.resolve(LocalDate.of(2099, 4, 1))).isEqualTo("2099-00");
    }
}
