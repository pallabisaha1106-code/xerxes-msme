package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.SlotCalculator.Busy;
import com.xerxes.msme.appointment.SlotCalculator.TimeRange;
import com.xerxes.msme.appointment.dto.AvailabilityResponse;
import com.xerxes.msme.appointment.dto.AvailableDayResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The free start times a shopper can pick from for one business: its published dates inside the booking window, each
 * laid out from the weekly hours on the business's appointment grid. Nothing here checks that the business is live
 * or bookable - that is the public side's job (see PublicAppointmentService).
 */
@Service
public class AppointmentAvailabilityService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentSettingsService settingsService;
    private final AppointmentHoursRepository hoursRepository;
    private final AppointmentOpenDayRepository openDayRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public AppointmentAvailabilityService(
            AppointmentSettingsService settingsService,
            AppointmentHoursRepository hoursRepository,
            AppointmentOpenDayRepository openDayRepository,
            AppointmentRepository appointmentRepository,
            Clock clock) {
        this.settingsService = settingsService;
        this.hoursRepository = hoursRepository;
        this.openDayRepository = openDayRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID tenantId) {
        int slotMinutes = settingsService.get(tenantId).slotMinutes();
        LocalDate first = AppointmentRules.today(clock);
        LocalDate last = first.plusDays(AppointmentRules.WINDOW_DAYS - 1);

        Map<Integer, List<TimeRange>> rangesByWeekday = hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId).stream()
                .collect(Collectors.groupingBy(
                        AppointmentHours::getDayOfWeek,
                        Collectors.mapping(hours -> new TimeRange(hours.getOpensAt(), hours.getClosesAt()), Collectors.toList())));
        List<Busy> busy = busyIntervals(tenantId, first, last);

        List<AvailableDayResponse> days = openDayRepository.findAllByTenantIdAndDayBetween(tenantId, first, last).stream()
                .map(AppointmentOpenDay::getDay)
                .sorted(Comparator.naturalOrder())
                .map(date -> new AvailableDayResponse(date, SlotCalculator
                        .freeStartTimes(date, slotMinutes, rangesByWeekday.getOrDefault(date.getDayOfWeek().getValue(), List.of()),
                                busy, clock.instant(), AppointmentRules.ZONE)
                        .stream()
                        .map(TIME::format)
                        .toList()))
                .filter(day -> !day.times().isEmpty())
                .toList();
        return new AvailabilityResponse(slotMinutes, days);
    }

    /** The free start times ("HH:mm") on one date; empty if the date is not published, outside the window, or full. */
    @Transactional(readOnly = true)
    public List<String> freeTimes(UUID tenantId, LocalDate date) {
        return availability(tenantId).days().stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .map(AvailableDayResponse::times)
                .orElse(List.of());
    }

    /** Time already taken by approved appointments from the start of {@code from} to the end of {@code to}. Waiting requests block nothing. */
    private List<Busy> busyIntervals(UUID tenantId, LocalDate from, LocalDate to) {
        return appointmentRepository
                .findOverlapping(
                        tenantId,
                        AppointmentStatus.APPROVED,
                        from.atStartOfDay(AppointmentRules.ZONE).toOffsetDateTime(),
                        to.plusDays(1).atStartOfDay(AppointmentRules.ZONE).toOffsetDateTime())
                .stream()
                .map(appointment -> new Busy(appointment.getStartsAt().toInstant(), appointment.getEndsAt().toInstant()))
                .toList();
    }
}
