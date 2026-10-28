package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AppointmentRulesTest {

    @Test
    void slotLengths_areTheAgreedList_andTheDefaultIsOneOfThem() {
        assertThat(AppointmentRules.SLOT_LENGTHS).containsExactly(10, 15, 20, 30, 45, 60, 90, 120);
        assertThat(AppointmentRules.isAllowedSlotLength(AppointmentRules.DEFAULT_SLOT_MINUTES)).isTrue();
    }

    @Test
    void isAllowedSlotLength_acceptsListedLengthsAndRejectsOthers() {
        assertThat(AppointmentRules.isAllowedSlotLength(45)).isTrue();
        assertThat(AppointmentRules.isAllowedSlotLength(25)).isFalse();
        assertThat(AppointmentRules.isAllowedSlotLength(0)).isFalse();
        assertThat(AppointmentRules.isAllowedSlotLength(-30)).isFalse();
    }

    @Test
    void windowAndNotice_areTheAgreedValues() {
        assertThat(AppointmentRules.WINDOW_DAYS).isEqualTo(14);
        assertThat(AppointmentRules.MIN_NOTICE).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void today_isTheBusinessesDate_notUtcs() {
        // 19:00 UTC on the 18th is 00:30 on the 19th in India.
        Clock lateEveningUtc = Clock.fixed(Instant.parse("2026-10-18T19:00:00Z"), ZoneOffset.UTC);

        assertThat(AppointmentRules.today(lateEveningUtc)).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void today_ignoresTheClocksOwnZone() {
        Instant instant = Instant.parse("2026-10-18T19:00:00Z");

        assertThat(AppointmentRules.today(Clock.fixed(instant, ZoneId.of("America/Los_Angeles"))))
                .isEqualTo(AppointmentRules.today(Clock.fixed(instant, ZoneOffset.UTC)));
    }

    @Test
    void clockBean_runsInIndiaTime() {
        assertThat(new AppointmentClockConfig().appointmentClock().getZone()).isEqualTo(ZoneId.of("Asia/Kolkata"));
    }
}
