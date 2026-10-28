package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void aPlainTenDigitMobileIsKept() {
        assertThat(PhoneNumbers.normalizeIndianMobile("9876543210")).contains("9876543210");
    }

    @Test
    void spacesDashesAndBracketsAreIgnored() {
        assertThat(PhoneNumbers.normalizeIndianMobile(" 98765 43210 ")).contains("9876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("98765-43210")).contains("9876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("(98765) 43210")).contains("9876543210");
    }

    @Test
    void aCountryCodeOrTrunkZeroIsDropped() {
        assertThat(PhoneNumbers.normalizeIndianMobile("+91 98765 43210")).contains("9876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("919876543210")).contains("9876543210");
        assertThat(PhoneNumbers.normalizeIndianMobile("09876543210")).contains("9876543210");
    }

    @Test
    void everyValidFirstDigitIsAccepted() {
        for (char first : "6789".toCharArray()) {
            assertThat(PhoneNumbers.normalizeIndianMobile(first + "123456789")).isPresent();
        }
    }

    @Test
    void numbersThatAreNotIndianMobilesAreRejected() {
        for (String bad : new String[] {"", "   ", "12345", "98765432", "987654321012", "5876543210", "0123456789",
                "abcdefghij", "98765 4321a", "+1 415 555 2671", "+91 5876543210", "9876543210 ext 5"}) {
            assertThat(PhoneNumbers.normalizeIndianMobile(bad)).as(bad).isEqualTo(Optional.empty());
        }
    }

    @Test
    void nullIsRejected() {
        assertThat(PhoneNumbers.normalizeIndianMobile(null)).isEmpty();
    }
}
