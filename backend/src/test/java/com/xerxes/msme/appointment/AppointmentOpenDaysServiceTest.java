package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AppointmentOpenDaysResponse;
import com.xerxes.msme.appointment.dto.OpenDayResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentOpenDaysServiceTest {

    // 19:00 UTC on Sunday the 18th is 00:30 on Monday the 19th in India, so "today" for the business is the 19th.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate LAST_DAY = TODAY.plusDays(13);

    @Mock private AppointmentOpenDayRepository openDayRepository;
    @Mock private AppointmentHoursRepository hoursRepository;

    private AppointmentOpenDaysService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-18T19:00:00Z"), ZoneOffset.UTC);
        service = new AppointmentOpenDaysService(openDayRepository, hoursRepository, clock);
        hoursOnWeekdays();
    }

    /** Hours on the given ISO weekdays (1 = Monday); with none given the business has no hours at all. */
    private void hoursOnWeekdays(int... weekdays) {
        List<AppointmentHours> rows = java.util.Arrays.stream(weekdays).mapToObj(day -> {
            AppointmentHours hours = new AppointmentHours();
            hours.setDayOfWeek(day);
            hours.setOpensAt(LocalTime.of(10, 0));
            hours.setClosesAt(LocalTime.of(13, 0));
            return hours;
        }).toList();
        lenient().when(hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId)).thenReturn(rows);
    }

    private static AppointmentOpenDay published(LocalDate date) {
        AppointmentOpenDay day = new AppointmentOpenDay();
        day.setDay(date);
        return day;
    }

    @Test
    void list_coversFourteenDaysStartingTodayInBusinessTime() {
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY)).thenReturn(List.of());

        List<OpenDayResponse> days = service.list(tenantId).days();

        assertThat(days).hasSize(14);
        assertThat(days.get(0).date()).isEqualTo(TODAY);
        assertThat(days.get(13).date()).isEqualTo(LAST_DAY);
        assertThat(days).noneMatch(OpenDayResponse::open);
    }

    @Test
    void list_marksPublishedDates() {
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY))
                .thenReturn(List.of(published(TODAY.plusDays(1)), published(TODAY.plusDays(5))));

        AppointmentOpenDaysResponse response = service.list(tenantId);

        assertThat(response.days()).filteredOn(OpenDayResponse::open).extracting(OpenDayResponse::date)
                .containsExactly(TODAY.plusDays(1), TODAY.plusDays(5));
    }

    @Test
    void list_flagsWeekdaysThatHaveHours() {
        hoursOnWeekdays(1, 2); // Monday and Tuesday
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY)).thenReturn(List.of());

        List<OpenDayResponse> days = service.list(tenantId).days();

        assertThat(days).filteredOn(OpenDayResponse::weekdayHasHours).extracting(OpenDayResponse::date)
                .containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(7), TODAY.plusDays(8));
    }

    @Test
    void list_noHoursAtAll_flagsNoWeekday() {
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY)).thenReturn(List.of());

        assertThat(service.list(tenantId).days()).noneMatch(OpenDayResponse::weekdayHasHours);
    }

    @Test
    void publish_savesTheDateForThatBusiness() {
        hoursOnWeekdays(2);
        LocalDate tuesday = TODAY.plusDays(1);
        when(openDayRepository.existsByTenantIdAndDay(tenantId, tuesday)).thenReturn(false);

        OpenDayResponse response = service.setOpen(tenantId, tuesday, true);

        ArgumentCaptor<AppointmentOpenDay> saved = ArgumentCaptor.forClass(AppointmentOpenDay.class);
        verify(openDayRepository).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getValue().getDay()).isEqualTo(tuesday);
        assertThat(response).isEqualTo(new OpenDayResponse(tuesday, true, true));
    }

    @Test
    void publish_alreadyPublished_savesNothingAgain() {
        when(openDayRepository.existsByTenantIdAndDay(tenantId, TODAY)).thenReturn(true);

        assertThat(service.setOpen(tenantId, TODAY, true).open()).isTrue();

        verify(openDayRepository, never()).save(any());
    }

    @Test
    void publish_todayAndTheLastDayOfTheWindow_areAccepted() {
        service.setOpen(tenantId, TODAY, true);
        service.setOpen(tenantId, LAST_DAY, true);

        verify(openDayRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void publish_outsideTheWindow_isRejectedAndNothingIsSaved() {
        BusinessRuleViolationException yesterday = assertThrows(
                BusinessRuleViolationException.class, () -> service.setOpen(tenantId, TODAY.minusDays(1), true));
        assertThat(yesterday.getMessage()).contains("2026-10-19").contains("2026-11-01");

        assertThrows(BusinessRuleViolationException.class, () -> service.setOpen(tenantId, LAST_DAY.plusDays(1), true));

        verify(openDayRepository, never()).save(any());
    }

    @Test
    void publish_weekdayWithoutHours_isAllowedButFlagged() {
        OpenDayResponse response = service.setOpen(tenantId, TODAY, true);

        assertThat(response.open()).isTrue();
        assertThat(response.weekdayHasHours()).isFalse();
    }

    @Test
    void unpublish_removesTheDate() {
        OpenDayResponse response = service.setOpen(tenantId, TODAY.plusDays(3), false);

        verify(openDayRepository).deleteByTenantIdAndDay(tenantId, TODAY.plusDays(3));
        assertThat(response.open()).isFalse();
    }

    @Test
    void unpublish_aDateOutsideTheWindow_isStillFine() {
        service.setOpen(tenantId, TODAY.minusDays(30), false);

        verify(openDayRepository).deleteByTenantIdAndDay(tenantId, TODAY.minusDays(30));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishNext_opensEveryDateWhoseWeekdayHasHours_forTheRequestedDays() {
        hoursOnWeekdays(1, 2, 3, 4, 5); // Monday to Friday; the 19th is a Monday
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY)).thenReturn(List.of());

        AppointmentOpenDaysResponse response = service.publishNext(tenantId, 7);

        ArgumentCaptor<List<AppointmentOpenDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(openDayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(AppointmentOpenDay::getDay)
                .containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3), TODAY.plusDays(4));
        assertThat(saved.getValue()).allSatisfy(day -> assertThat(day.getTenantId()).isEqualTo(tenantId));
        // The weekend inside the 7 days stays closed, and so does everything after them.
        assertThat(response.days()).filteredOn(OpenDayResponse::open).hasSize(5);
        assertThat(response.days().get(5).open()).isFalse();
        assertThat(response.days().get(8).open()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishNext_leavesAlreadyPublishedDatesAlone_butStillReportsThemOpen() {
        hoursOnWeekdays(1, 2, 3);
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY))
                .thenReturn(List.of(published(TODAY.plusDays(1)), published(TODAY.plusDays(9))));

        AppointmentOpenDaysResponse response = service.publishNext(tenantId, 3);

        ArgumentCaptor<List<AppointmentOpenDay>> saved = ArgumentCaptor.forClass(List.class);
        verify(openDayRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(AppointmentOpenDay::getDay).containsExactly(TODAY, TODAY.plusDays(2));
        // The earlier-published date inside the window is still open, as is the one beyond the 3 days.
        assertThat(response.days()).filteredOn(OpenDayResponse::open).extracting(OpenDayResponse::date)
                .containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(9));
    }

    @Test
    void publishNext_noDaysOutOfRange_isRejected() {
        hoursOnWeekdays(1);

        assertThrows(BusinessRuleViolationException.class, () -> service.publishNext(tenantId, 0));
        assertThrows(BusinessRuleViolationException.class, () -> service.publishNext(tenantId, 15));
        verify(openDayRepository, never()).saveAll(any());
    }

    @Test
    void publishNext_fourteenDays_isTheWholeWindow() {
        hoursOnWeekdays(1, 2, 3, 4, 5, 6, 7);
        when(openDayRepository.findAllByTenantIdAndDayBetween(tenantId, TODAY, LAST_DAY)).thenReturn(List.of());

        assertThat(service.publishNext(tenantId, 14).days()).allMatch(OpenDayResponse::open);
    }

    @Test
    void publishNext_withNoWeeklyHours_isRejectedWithAClearMessage() {
        BusinessRuleViolationException error =
                assertThrows(BusinessRuleViolationException.class, () -> service.publishNext(tenantId, 7));

        assertThat(error.getMessage()).isEqualTo("Set your weekly opening hours first.");
        verify(openDayRepository, never()).saveAll(any());
    }
}
