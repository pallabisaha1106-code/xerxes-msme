package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AmountInWordsConverterTest {

    @Test
    void toWords_zero_returnsZeroOnly() {
        assertThat(AmountInWordsConverter.toWords(BigDecimal.ZERO)).isEqualTo("Rupees Zero Only");
    }

    @Test
    void toWords_singleDigit_spellsOutOnes() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("7"))).isEqualTo("Rupees Seven Only");
    }

    @Test
    void toWords_teens_usesIrregularNames() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("15"))).isEqualTo("Rupees Fifteen Only");
    }

    @Test
    void toWords_tensAndOnes_combinesCorrectly() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("456"))).isEqualTo("Rupees Four Hundred Fifty Six Only");
    }

    @Test
    void toWords_exactTens_omitsOnesWord() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("450"))).isEqualTo("Rupees Four Hundred Fifty Only");
    }

    @Test
    void toWords_thousands_usesIndianGrouping() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("1234")))
                .isEqualTo("Rupees One Thousand Two Hundred Thirty Four Only");
    }

    @Test
    void toWords_lakh_usesIndianNumberingNotMillion() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("123456")))
                .isEqualTo("Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six Only");
    }

    @Test
    void toWords_crore_usesIndianNumbering() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("12345678")))
                .isEqualTo("Rupees One Crore Twenty Three Lakh Forty Five Thousand Six Hundred Seventy Eight Only");
    }

    @Test
    void toWords_paise_roundsToNearestRupee() {
        assertThat(AmountInWordsConverter.toWords(new BigDecimal("999.60"))).isEqualTo("Rupees One Thousand Only");
    }
}
