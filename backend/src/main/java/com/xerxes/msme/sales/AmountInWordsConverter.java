package com.xerxes.msme.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Spells out a rupee amount using the Indian numbering system (lakh/crore,
 * not thousand/million) - the convention Indian GST invoices are expected to
 * follow for the "Amount in words" line. Paise are dropped; the amount is
 * rounded to the nearest rupee before conversion.
 */
public final class AmountInWordsConverter {

    private AmountInWordsConverter() {}

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
        "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String toWords(BigDecimal amount) {
        long rupees = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (rupees == 0) {
            return "Rupees Zero Only";
        }

        StringBuilder words = new StringBuilder("Rupees ");
        long crore = rupees / 10000000;
        rupees %= 10000000;
        long lakh = rupees / 100000;
        rupees %= 100000;
        long thousand = rupees / 1000;
        rupees %= 1000;
        long hundred = rupees / 100;
        long remainder = rupees % 100;

        if (crore > 0) {
            words.append(twoDigits(crore)).append(" Crore ");
        }
        if (lakh > 0) {
            words.append(twoDigits(lakh)).append(" Lakh ");
        }
        if (thousand > 0) {
            words.append(twoDigits(thousand)).append(" Thousand ");
        }
        if (hundred > 0) {
            words.append(ONES[(int) hundred]).append(" Hundred ");
        }
        if (remainder > 0) {
            words.append(twoDigits(remainder)).append(" ");
        }

        words.append("Only");
        return words.toString().replaceAll(" {2,}", " ").trim();
    }

    private static String twoDigits(long value) {
        if (value < 20) {
            return ONES[(int) value];
        }
        long tens = value / 10;
        long ones = value % 10;
        return ones == 0 ? TENS[(int) tens] : TENS[(int) tens] + " " + ONES[(int) ones];
    }
}
