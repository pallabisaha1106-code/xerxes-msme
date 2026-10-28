package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AppointmentHoursResponse;
import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.appointment.dto.OpeningRangeRequest;
import com.xerxes.msme.appointment.dto.OpeningRangeResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentHoursServiceTest {

    @Mock private AppointmentHoursRepository hoursRepository;
    @Mock private AppointmentSettingsService settingsService;

    private AppointmentHoursService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AppointmentHoursService(hoursRepository, settingsService);
        useSlotMinutes(30);
    }

    private void useSlotMinutes(int minutes) {
        lenient().when(settingsService.get(tenantId)).thenReturn(new AppointmentSettingsResponse(minutes, AppointmentRules.SLOT_LENGTHS));
    }

    private static OpeningRangeRequest range(int day, String opens, String closes) {
        return new OpeningRangeRequest(day, LocalTime.parse(opens), LocalTime.parse(closes));
    }

    private static AppointmentHours row(int day, String opens, String closes) {
        AppointmentHours hours = new AppointmentHours();
        hours.setDayOfWeek(day);
        hours.setOpensAt(LocalTime.parse(opens));
        hours.setClosesAt(LocalTime.parse(closes));
        return hours;
    }

    private void assertRejected(String messagePart, OpeningRangeRequest... ranges) {
        BusinessRuleViolationException error =
                assertThrows(BusinessRuleViolationException.class, () -> service.replaceWeek(tenantId, List.of(ranges)));
        assertThat(error.getMessage()).contains(messagePart);
        verify(hoursRepository, never()).deleteAllByTenantId(any());
        verify(hoursRepository, never()).saveAll(any());
    }

    @Test
    void getWeek_returnsTheStoredRangesInOrder() {
        when(hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId))
                .thenReturn(List.of(row(1, "10:00", "13:00"), row(1, "17:00", "20:00"), row(2, "09:30", "12:00")));

        AppointmentHoursResponse response = service.getWeek(tenantId);

        assertThat(response.ranges()).containsExactly(
                new OpeningRangeResponse(1, LocalTime.of(10, 0), LocalTime.of(13, 0)),
                new OpeningRangeResponse(1, LocalTime.of(17, 0), LocalTime.of(20, 0)),
                new OpeningRangeResponse(2, LocalTime.of(9, 30), LocalTime.of(12, 0)));
    }

    @Test
    void getWeek_nothingSaved_isEmpty() {
        when(hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId)).thenReturn(List.of());

        assertThat(service.getWeek(tenantId).ranges()).isEmpty();
    }

    @Test
    void replaceWeek_removesTheOldWeekThenSavesTheNewRangesForThatBusiness() {
        AppointmentHoursResponse response =
                service.replaceWeek(tenantId, List.of(range(2, "10:00", "13:00"), range(1, "17:00", "20:00"), range(1, "10:00", "13:00")));

        InOrder order = inOrder(hoursRepository);
        order.verify(hoursRepository).deleteAllByTenantId(tenantId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AppointmentHours>> saved = ArgumentCaptor.forClass(List.class);
        order.verify(hoursRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(3).allSatisfy(hours -> assertThat(hours.getTenantId()).isEqualTo(tenantId));
        assertThat(saved.getValue()).extracting(AppointmentHours::getDayOfWeek).containsExactly(1, 1, 2);
        assertThat(response.ranges()).extracting(OpeningRangeResponse::opensAt)
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(17, 0), LocalTime.of(10, 0));
    }

    @Test
    void replaceWeek_emptyList_clearsTheWeek() {
        AppointmentHoursResponse response = service.replaceWeek(tenantId, List.of());

        verify(hoursRepository).deleteAllByTenantId(tenantId);
        assertThat(response.ranges()).isEmpty();
    }

    @Test
    void replaceWeek_splitShiftsAndTouchingRangesAreFine() {
        service.replaceWeek(tenantId, List.of(range(1, "10:00", "13:00"), range(1, "13:00", "15:00"), range(1, "17:00", "20:00")));

        verify(hoursRepository).saveAll(any());
    }

    @Test
    void replaceWeek_openingNotBeforeClosing_isRejectedNamingTheDay() {
        assertRejected("Monday: the opening time must be before the closing time", range(1, "13:00", "10:00"));
        assertRejected("Tuesday: the opening time must be before the closing time", range(2, "10:00", "10:00"));
    }

    @Test
    void replaceWeek_overlappingRangesOnOneDay_areRejected() {
        assertRejected("Wednesday: opening ranges must not overlap", range(3, "10:00", "13:00"), range(3, "12:30", "15:00"));
    }

    @Test
    void replaceWeek_sameTimesOnDifferentDays_isNotAnOverlap() {
        service.replaceWeek(tenantId, List.of(range(1, "10:00", "13:00"), range(2, "10:00", "13:00")));

        verify(hoursRepository).saveAll(any());
    }

    @Test
    void replaceWeek_moreThanFourRangesInADay_isRejected() {
        assertRejected("Friday: at most 4 opening ranges per day",
                range(5, "08:00", "09:00"), range(5, "10:00", "11:00"), range(5, "12:00", "13:00"),
                range(5, "14:00", "15:00"), range(5, "16:00", "17:00"));
    }

    @Test
    void replaceWeek_fourRangesInADay_isFine() {
        service.replaceWeek(tenantId, List.of(range(5, "08:00", "09:00"), range(5, "10:00", "11:00"), range(5, "12:00", "13:00"), range(5, "14:00", "15:00")));

        verify(hoursRepository).saveAll(any());
    }

    @Test
    void replaceWeek_rangeShorterThanOneAppointment_isRejected() {
        assertRejected("Thursday: each opening range must be at least one appointment (30 minutes) long", range(4, "10:00", "10:20"));
    }

    @Test
    void replaceWeek_rangeExactlyOneAppointmentLong_isFine() {
        service.replaceWeek(tenantId, List.of(range(4, "10:00", "10:30")));

        verify(hoursRepository).saveAll(any());
    }

    @Test
    void replaceWeek_usesTheBusinessesOwnAppointmentLength() {
        useSlotMinutes(45);

        assertRejected("(45 minutes)", range(1, "10:00", "10:30"));
    }
}
