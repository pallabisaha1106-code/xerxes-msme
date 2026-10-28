package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.xerxes.msme.appointment.SlotCalculator.Busy;
import com.xerxes.msme.appointment.SlotCalculator.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlotCalculatorTest {

    private static final ZoneId INDIA = AppointmentRules.ZONE;
    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);
    /** Long before the day, so the minimum notice never matters unless a test moves "now". */
    private static final Instant EARLIER = at(LocalDate.of(2026, 10, 19), "00:00");

    private static Instant at(LocalDate date, String time) {
        return ZonedDateTime.of(date, LocalTime.parse(time), INDIA).toInstant();
    }

    private static TimeRange range(String from, String to) {
        return new TimeRange(LocalTime.parse(from), LocalTime.parse(to));
    }

    private static Busy busy(String from, String to) {
        return new Busy(at(DAY, from), at(DAY, to));
    }

    private static List<String> times(int slotMinutes, List<TimeRange> ranges, List<Busy> busy, Instant now) {
        return SlotCalculator.freeStartTimes(DAY, slotMinutes, ranges, busy, now, INDIA).stream().map(LocalTime::toString).toList();
    }

    private static List<String> times(int slotMinutes, TimeRange... ranges) {
        return times(slotMinutes, List.of(ranges), List.of(), EARLIER);
    }

    // ---- the grid ----

    @Test
    void startsStepThroughTheRangeInAppointmentLengthSteps() {
        assertThat(times(30, range("10:00", "13:00"))).containsExactly("10:00", "10:30", "11:00", "11:30", "12:00", "12:30");
    }

    @Test
    void aStartCountsOnlyIfAWholeAppointmentFitsBeforeClosing() {
        // 45-minute appointments in a two-hour range: 11:30 would run to 12:15.
        assertThat(times(45, range("10:00", "12:00"))).containsExactly("10:00", "10:45");
    }

    @Test
    void theGridIsAnchoredAtEachRangesOwnOpeningTime() {
        assertThat(times(30, range("09:15", "10:45"))).containsExactly("09:15", "09:45", "10:15");
    }

    @Test
    void aRangeExactlyOneAppointmentLongGivesOneStart() {
        assertThat(times(30, range("10:00", "10:30"))).containsExactly("10:00");
    }

    @Test
    void aRangeShorterThanOneAppointmentGivesNone() {
        assertThat(times(30, range("10:00", "10:20"))).isEmpty();
    }

    @Test
    void noRanges_meansNoStarts() {
        assertThat(times(30)).isEmpty();
    }

    @Test
    void splitShiftsGiveStartsFromBothRanges() {
        assertThat(times(60, range("10:00", "13:00"), range("17:00", "20:00")))
                .containsExactly("10:00", "11:00", "12:00", "17:00", "18:00", "19:00");
    }

    @Test
    void touchingRangesGiveEachStartOnce_inOrder() {
        assertThat(times(60, range("12:00", "14:00"), range("10:00", "12:00"))).containsExactly("10:00", "11:00", "12:00", "13:00");
    }

    @Test
    void aRangeRunningToTheEndOfTheDayDoesNotWrapIntoTheNextOne() {
        // The last start is 23:00; 23:30 would end at midnight, past the 23:59 closing.
        assertThat(times(30, range("22:00", "23:59"))).containsExactly("22:00", "22:30", "23:00");
    }

    @Test
    void everyAllowedLengthWorks() {
        for (int minutes : AppointmentRules.SLOT_LENGTHS) {
            List<String> starts = times(minutes, range("08:00", "20:00"));
            assertThat(starts).isNotEmpty().first().isEqualTo("08:00");
            LocalTime last = LocalTime.parse(starts.get(starts.size() - 1));
            assertThat(last.plusMinutes(minutes)).isBeforeOrEqualTo(LocalTime.of(20, 0));
            assertThat(last.plusMinutes(2L * minutes)).isAfter(LocalTime.of(20, 0)); // no further start would have fit
        }
    }

    // ---- minimum notice ----

    @Test
    void aStartExactlyTheMinimumNoticeAwayIsStillOffered() {
        Instant eightAm = at(DAY, "08:00"); // two hours' notice means 10:00 is the earliest
        assertThat(times(30, List.of(range("09:00", "12:00")), List.of(), eightAm)).containsExactly("10:00", "10:30", "11:00", "11:30");
    }

    @Test
    void oneMinuteInsideTheNoticeDropsThatStart() {
        Instant justAfterEightAm = at(DAY, "08:01");
        assertThat(times(30, List.of(range("09:00", "12:00")), List.of(), justAfterEightAm)).containsExactly("10:30", "11:00", "11:30");
    }

    @Test
    void aWholeDayAlreadyInThePastHasNoStarts() {
        Instant dayAfter = at(DAY.plusDays(1), "09:00");
        assertThat(times(30, List.of(range("10:00", "13:00")), List.of(), dayAfter)).isEmpty();
    }

    @Test
    void noticeIsCountedInBusinessTime_notUtc() {
        // 04:30 UTC is 10:00 in India, so 12:00 India is the earliest start.
        Instant tenAmIndia = Instant.parse("2026-10-21T04:30:00Z");
        assertThat(times(30, List.of(range("10:00", "13:00")), List.of(), tenAmIndia)).containsExactly("12:00", "12:30");
    }

    // ---- busy intervals ----

    @Test
    void anAppointmentTakingASlotRemovesOnlyThatStart() {
        assertThat(times(30, List.of(range("10:00", "12:00")), List.of(busy("11:00", "11:30")), EARLIER))
                .containsExactly("10:00", "10:30", "11:30");
    }

    @Test
    void aPartialOverlapRemovesEveryStartItTouches() {
        // 10:15-10:45 cuts into both the 10:00 and the 10:30 appointments.
        assertThat(times(30, List.of(range("10:00", "12:00")), List.of(busy("10:15", "10:45")), EARLIER))
                .containsExactly("11:00", "11:30");
    }

    @Test
    void anIntervalThatOnlyTouchesDoesNotRemoveTheNeighbouringStarts() {
        // 10:30-11:00 ends exactly where 11:00 begins and starts exactly where 10:00 ends.
        assertThat(times(30, List.of(range("10:00", "12:00")), List.of(busy("10:30", "11:00")), EARLIER))
                .containsExactly("10:00", "11:00", "11:30");
    }

    @Test
    void aLongerBusyIntervalRemovesSeveralStarts() {
        assertThat(times(30, List.of(range("10:00", "12:00")), List.of(busy("10:00", "11:30")), EARLIER)).containsExactly("11:30");
    }

    @Test
    void severalBusyIntervalsAreAllHonoured() {
        assertThat(times(30, List.of(range("10:00", "12:00")), List.of(busy("10:00", "10:30"), busy("11:30", "12:00")), EARLIER))
                .containsExactly("10:30", "11:00");
    }

    @Test
    void aBusyIntervalCoveringTheWholeDayLeavesNothing() {
        assertThat(times(30, List.of(range("10:00", "13:00")), List.of(busy("00:00", "23:59")), EARLIER)).isEmpty();
    }

    @Test
    void busyTimeOnAnotherDateChangesNothing() {
        Busy otherDay = new Busy(at(DAY.plusDays(1), "10:00"), at(DAY.plusDays(1), "11:00"));
        assertThat(times(30, List.of(range("10:00", "11:00")), List.of(otherDay), EARLIER)).containsExactly("10:00", "10:30");
    }

    @Test
    void onALongerGridAnOverlapWithTheTailOfAnAppointmentStillBlocksIt() {
        // Starts are 10:00 and 10:45; the second would run 10:45-11:30, so a busy 11:15-11:30 blocks it.
        assertThat(times(45, List.of(range("10:00", "12:00")), List.of(busy("11:15", "11:30")), EARLIER)).containsExactly("10:00");
    }

    @Test
    void busyIntervalsAreMatchedInRealTime_soUtcInstantsLineUpWithIndiaTimes() {
        // 04:30Z-05:00Z is 10:00-10:30 in India.
        Busy fromUtc = new Busy(Instant.parse("2026-10-21T04:30:00Z"), Instant.parse("2026-10-21T05:00:00Z"));
        assertThat(times(30, List.of(range("10:00", "11:00")), List.of(fromUtc), EARLIER)).containsExactly("10:30");
    }
}
