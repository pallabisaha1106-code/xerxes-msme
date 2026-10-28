package com.xerxes.msme.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Works out which appointment start times are free on one date. Pure: no database and no clock of its own, so every
 * rule can be tested exactly.
 *
 * <p>Starts step through each opening range from its opening time in {@code slotMinutes} steps, and a start counts
 * only if a whole appointment still fits before the range closes. A start is then dropped if it is sooner than the
 * minimum notice from {@code now}, or if its appointment overlaps a busy interval (one that merely touches does not).
 */
public final class SlotCalculator {

    /** A span of local time within one day, e.g. 10:00 to 13:00. */
    public record TimeRange(LocalTime from, LocalTime to) {}

    /** A span of real time that is already taken, e.g. an approved appointment. */
    public record Busy(Instant start, Instant end) {}

    private SlotCalculator() {}

    public static SortedSet<LocalTime> freeStartTimes(
            LocalDate date, int slotMinutes, List<TimeRange> ranges, List<Busy> busy, Instant now, ZoneId zone) {
        Instant earliest = now.plus(AppointmentRules.MIN_NOTICE);
        SortedSet<LocalTime> free = new TreeSet<>();
        for (TimeRange range : ranges) {
            // Whole minutes since midnight, so a range ending at 23:59 can't wrap around into the next day.
            int closes = range.to().toSecondOfDay() / 60;
            for (int start = range.from().toSecondOfDay() / 60; start + slotMinutes <= closes; start += slotMinutes) {
                LocalTime time = LocalTime.ofSecondOfDay(start * 60L);
                Instant begins = date.atTime(time).atZone(zone).toInstant();
                Instant ends = begins.plusSeconds(slotMinutes * 60L);
                if (!begins.isBefore(earliest) && busy.stream().noneMatch(b -> begins.isBefore(b.end()) && ends.isAfter(b.start()))) {
                    free.add(time);
                }
            }
        }
        return free;
    }
}
