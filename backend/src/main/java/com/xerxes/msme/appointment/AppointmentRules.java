package com.xerxes.msme.appointment;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * The fixed rules of appointment booking - the same for every business, so plain constants rather than settings.
 * All dates and times are local to one time zone (India) in v1.
 */
public final class AppointmentRules {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    /** The appointment lengths, in minutes, a business can choose between. */
    public static final List<Integer> SLOT_LENGTHS = List.of(10, 15, 20, 30, 45, 60, 90, 120);

    /** What a business that has not chosen yet gets. */
    public static final int DEFAULT_SLOT_MINUTES = 30;

    /** Shoppers can request this many days, counting today. */
    public static final int WINDOW_DAYS = 14;

    /** A slot stops being offered this long before it starts, so the business has time to see the request. */
    public static final Duration MIN_NOTICE = Duration.ofHours(2);

    /** Opening ranges per weekday (a split shift needs two or three). */
    public static final int MAX_RANGES_PER_DAY = 4;

    /** Pending requests one phone number can have with one business at a time. */
    public static final int MAX_PENDING_PER_PHONE = 3;

    private AppointmentRules() {}

    public static boolean isAllowedSlotLength(int minutes) {
        return SLOT_LENGTHS.contains(minutes);
    }

    /** Today's date as the business sees it; the clock decides the instant, the rules decide the zone. */
    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
