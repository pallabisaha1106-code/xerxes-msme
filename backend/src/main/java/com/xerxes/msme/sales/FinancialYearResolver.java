package com.xerxes.msme.sales;

import java.time.LocalDate;
import java.time.Month;

/** India's GST financial year runs 1 April - 31 March, formatted like "2025-26". */
public final class FinancialYearResolver {

    private FinancialYearResolver() {}

    public static String resolve(LocalDate date) {
        int startYear = date.getMonthValue() >= Month.APRIL.getValue() ? date.getYear() : date.getYear() - 1;
        int endYearShort = (startYear + 1) % 100;
        return String.format("%04d-%02d", startYear, endYearShort);
    }
}
