package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.appointment.dto.AvailabilityResponse;
import com.xerxes.msme.appointment.dto.AvailableDayResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentAvailabilityServiceTest {

    // Sunday 18th 19:00 UTC is 00:30 on Monday the 19th in India, so the booking window is the 19th to 1 November.
    private static final Instant MONDAY_00_30_INDIA = Instant.parse("2026-10-18T19:00:00Z");
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate LAST_DAY = MONDAY.plusDays(13);

    @Mock private AppointmentSettingsService settingsService;
    @Mock private AppointmentHoursRepository hoursRepository;
    @Mock private AppointmentOpenDayRepository openDayRepository;
    @Mock private AppointmentRepository appointmentRepository;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useSlotMinutes(30);
        hours(row(1, "10:00", "12:00"), row(2, "10:00", "11:00")); // Monday 10-12, Tuesday 10-11
    }

    private AppointmentAvailabilityService service(Instant now) {
        return new AppointmentAvailabilityService(settingsService, hoursRepository, openDayRepository, appointmentRepository, Clock.fixed(now, ZoneOffset.UTC));
    }

    private void useSlotMinutes(int minutes) {
        lenient().when(settingsService.get(tenantId)).thenReturn(new AppointmentSettingsResponse(minutes, AppointmentRules.SLOT_LENGTHS));
    }

    private static AppointmentHours row(int weekday, String opens, String closes) {
        AppointmentHours hours = new AppointmentHours();
        hours.setDayOfWeek(weekday);
        hours.setOpensAt(LocalTime.parse(opens));
        hours.setClosesAt(LocalTime.parse(closes));
        return hours;
    }

    private void hours(AppointmentHours... rows) {
        lenient().when(hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId)).thenReturn(List.of(rows));
    }

    private void published(LocalDate... dates) {
        List<AppointmentOpenDay> rows = new ArrayList<>();
        for (LocalDate date : dates) {
            AppointmentOpenDay day = new AppointmentOpenDay();
            day.setDay(date);
            rows.add(day);
        }
        lenient().when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, MONDAY, LAST_DAY)).thenReturn(rows);
    }

    @Test
    void publishedDatesGetTheirTimesFromTheWeekdaysHours_earliestDateFirst() {
        published(MONDAY.plusDays(7), MONDAY, MONDAY.plusDays(1)); // next Monday, Monday, Tuesday - out of order

        AvailabilityResponse response = service(MONDAY_00_30_INDIA).availability(tenantId);

        assertThat(response.slotMinutes()).isEqualTo(30);
        assertThat(response.days()).containsExactly(
                new AvailableDayResponse(MONDAY, List.of("10:00", "10:30", "11:00", "11:30")),
                new AvailableDayResponse(MONDAY.plusDays(1), List.of("10:00", "10:30")),
                new AvailableDayResponse(MONDAY.plusDays(7), List.of("10:00", "10:30", "11:00", "11:30")));
    }

    @Test
    void aPublishedDateWhoseWeekdayHasNoHoursIsLeftOut() {
        published(MONDAY.plusDays(2)); // Wednesday: no hours

        assertThat(service(MONDAY_00_30_INDIA).availability(tenantId).days()).isEmpty();
    }

    @Test
    void unpublishedDatesNeverAppear() {
        published(MONDAY);

        assertThat(service(MONDAY_00_30_INDIA).availability(tenantId).days()).extracting(AvailableDayResponse::date).containsExactly(MONDAY);
    }

    @Test
    void nothingPublished_givesNoDays_butStillSaysHowLongAppointmentsAre() {
        published();

        AvailabilityResponse response = service(MONDAY_00_30_INDIA).availability(tenantId);

        assertThat(response.days()).isEmpty();
        assertThat(response.slotMinutes()).isEqualTo(30);
    }

    @Test
    void aDateWhoseSlotsAreAllPastTheNoticeCutoffIsLeftOut() {
        published(MONDAY, MONDAY.plusDays(1));
        // 10:00 on Monday in India: the earliest start is 12:00, after Monday's last one (11:30) - Tuesday remains.
        Instant tenAmIndia = Instant.parse("2026-10-19T04:30:00Z");

        assertThat(service(tenAmIndia).availability(tenantId).days()).extracting(AvailableDayResponse::date)
                .containsExactly(MONDAY.plusDays(1));
    }

    @Test
    void theBusinessesOwnAppointmentLengthShapesTheTimes() {
        useSlotMinutes(45);
        published(MONDAY);

        AvailabilityResponse response = service(MONDAY_00_30_INDIA).availability(tenantId);

        assertThat(response.slotMinutes()).isEqualTo(45);
        assertThat(response.days().get(0).times()).containsExactly("10:00", "10:45"); // 11:30 would end at 12:15
    }

    @Test
    void splitShiftsOnOneWeekdayAreBothUsed() {
        hours(row(1, "10:00", "11:00"), row(1, "17:00", "18:00"));
        published(MONDAY);

        assertThat(service(MONDAY_00_30_INDIA).availability(tenantId).days().get(0).times())
                .containsExactly("10:00", "10:30", "17:00", "17:30");
    }

    @Test
    void onlyDatesInsideTheFourteenDayWindowAreAskedFor() {
        published();

        service(MONDAY_00_30_INDIA).availability(tenantId);

        verify(openDayRepository).findAllByTenantIdAndDayBetween(tenantId, MONDAY, LAST_DAY);
    }

    @Test
    void noWeeklyHoursAtAll_givesNoDays() {
        hours();
        published(MONDAY);

        assertThat(service(MONDAY_00_30_INDIA).availability(tenantId).days()).isEmpty();
    }

    private static Appointment approvedAt(LocalDate date, String from, String to) {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.APPROVED);
        appointment.setStartsAt(date.atTime(LocalTime.parse(from)).atZone(AppointmentRules.ZONE).toOffsetDateTime());
        appointment.setEndsAt(date.atTime(LocalTime.parse(to)).atZone(AppointmentRules.ZONE).toOffsetDateTime());
        return appointment;
    }

    @Test
    void anApprovedAppointmentTakesItsTimeOffTheList() {
        published(MONDAY);
        when(appointmentRepository.findOverlapping(org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.eq(AppointmentStatus.APPROVED),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(approvedAt(MONDAY, "10:30", "11:00")));

        assertThat(service(MONDAY_00_30_INDIA).availability(tenantId).days().get(0).times()).containsExactly("10:00", "11:00", "11:30");
    }

    @Test
    void approvedAppointmentsAreLookedUpAcrossTheWholeWindowInBusinessTime() {
        published();

        service(MONDAY_00_30_INDIA).availability(tenantId);

        verify(appointmentRepository).findOverlapping(
                tenantId,
                AppointmentStatus.APPROVED,
                MONDAY.atStartOfDay(AppointmentRules.ZONE).toOffsetDateTime(),
                LAST_DAY.plusDays(1).atStartOfDay(AppointmentRules.ZONE).toOffsetDateTime());
    }

    @Test
    void aFullyBookedPublishedDateIsLeftOut() {
        published(MONDAY.plusDays(1)); // Tuesday 10:00-11:00 with two 30-minute slots
        when(appointmentRepository.findOverlapping(org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.eq(AppointmentStatus.APPROVED),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(approvedAt(MONDAY.plusDays(1), "10:00", "11:00")));

        assertThat(service(MONDAY_00_30_INDIA).availability(tenantId).days()).isEmpty();
    }

    @Test
    void freeTimes_isTheListForThatDate_andEmptyWhenTheDateIsNotPublished() {
        published(MONDAY);
        AppointmentAvailabilityService service = service(MONDAY_00_30_INDIA);

        assertThat(service.freeTimes(tenantId, MONDAY)).containsExactly("10:00", "10:30", "11:00", "11:30");
        assertThat(service.freeTimes(tenantId, MONDAY.plusDays(1))).isEmpty();
    }
}
