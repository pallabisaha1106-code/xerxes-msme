package com.xerxes.msme.appointment;

import java.util.Optional;

/** Phone numbers as shoppers type them. Appointment booking accepts Indian mobile numbers only in v1. */
public final class PhoneNumbers {

    private PhoneNumbers() {}

    /**
     * The 10-digit mobile number inside what the shopper typed: spaces, dashes and brackets are ignored, and a leading
     * "+91", "91" or "0" is dropped. Empty unless the result is ten digits starting with 6, 7, 8 or 9.
     */
    public static Optional<String> normalizeIndianMobile(String raw) {
        if (raw == null) return Optional.empty();
        String digits = raw.replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits.matches("[6-9][0-9]{9}") ? Optional.of(digits) : Optional.empty();
    }
}
