package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentOpenDaysResponse;
import com.xerxes.msme.appointment.dto.OpenDayResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The dates a business has published for appointments. Shoppers can request only published dates inside the booking
 * window (today and the 13 days after it), so a holiday is simply a date the business leaves unpublished.
 */
@Service
public class AppointmentOpenDaysService {

    private final AppointmentOpenDayRepository openDayRepository;
    private final AppointmentHoursRepository hoursRepository;
    private final Clock clock;

    public AppointmentOpenDaysService(
            AppointmentOpenDayRepository openDayRepository, AppointmentHoursRepository hoursRepository, Clock clock) {
        this.openDayRepository = openDayRepository;
        this.hoursRepository = hoursRepository;
        this.clock = clock;
    }

    /** Every date in the booking window, earliest first, with whether it is published and whether its weekday has hours. */
    @Transactional(readOnly = true)
    public AppointmentOpenDaysResponse list(UUID tenantId) {
        LocalDate first = AppointmentRules.today(clock);
        Set<LocalDate> published = openDayRepository.findAllByTenantIdAndDayBetween(tenantId, first, lastDay(first)).stream()
                .map(AppointmentOpenDay::getDay)
                .collect(Collectors.toSet());
        return window(first, published, weekdaysWithHours(tenantId));
    }

    /**
     * The "publish the next week" shortcut: publishes every date from today for {@code days} days whose weekday has
     * opening hours (dates already published are left alone) and returns the updated booking window.
     */
    @Transactional
    public AppointmentOpenDaysResponse publishNext(UUID tenantId, int days) {
        if (days < 1 || days > AppointmentRules.WINDOW_DAYS) {
            throw new BusinessRuleViolationException("Choose between 1 and " + AppointmentRules.WINDOW_DAYS + " days.");
        }
        Set<Integer> openWeekdays = weekdaysWithHours(tenantId);
        if (openWeekdays.isEmpty()) {
            throw new BusinessRuleViolationException("Set your weekly opening hours first.");
        }
        LocalDate first = AppointmentRules.today(clock);
        Set<LocalDate> published = openDayRepository.findAllByTenantIdAndDayBetween(tenantId, first, lastDay(first)).stream()
                .map(AppointmentOpenDay::getDay)
                .collect(Collectors.toCollection(HashSet::new));
        List<AppointmentOpenDay> added = IntStream.range(0, days)
                .mapToObj(first::plusDays)
                .filter(date -> openWeekdays.contains(date.getDayOfWeek().getValue()) && !published.contains(date))
                .map(date -> {
                    AppointmentOpenDay day = new AppointmentOpenDay();
                    day.setTenantId(tenantId);
                    day.setDay(date);
                    return day;
                })
                .toList();
        openDayRepository.saveAll(added);
        added.forEach(day -> published.add(day.getDay()));
        return window(first, published, openWeekdays);
    }

    private static AppointmentOpenDaysResponse window(LocalDate first, Set<LocalDate> published, Set<Integer> openWeekdays) {
        List<OpenDayResponse> days = IntStream.range(0, AppointmentRules.WINDOW_DAYS)
                .mapToObj(first::plusDays)
                .map(date -> new OpenDayResponse(date, published.contains(date), openWeekdays.contains(date.getDayOfWeek().getValue())))
                .toList();
        return new AppointmentOpenDaysResponse(days);
    }

    /**
     * Publishes or unpublishes one date; doing either twice is harmless. Only dates inside the booking window can be
     * published. A date whose weekday has no hours can still be published - it just shows no slots until hours exist.
     */
    @Transactional
    public OpenDayResponse setOpen(UUID tenantId, LocalDate date, boolean open) {
        if (open) {
            LocalDate first = AppointmentRules.today(clock);
            if (date.isBefore(first) || date.isAfter(lastDay(first))) {
                throw new BusinessRuleViolationException(
                        "You can publish dates from " + first + " to " + lastDay(first) + " only.");
            }
            if (!openDayRepository.existsByTenantIdAndDay(tenantId, date)) {
                AppointmentOpenDay day = new AppointmentOpenDay();
                day.setTenantId(tenantId);
                day.setDay(date);
                openDayRepository.save(day);
            }
        } else {
            openDayRepository.deleteByTenantIdAndDay(tenantId, date);
        }
        return new OpenDayResponse(date, open, weekdaysWithHours(tenantId).contains(date.getDayOfWeek().getValue()));
    }

    private static LocalDate lastDay(LocalDate first) {
        return first.plusDays(AppointmentRules.WINDOW_DAYS - 1);
    }

    private Set<Integer> weekdaysWithHours(UUID tenantId) {
        return hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId).stream()
                .map(AppointmentHours::getDayOfWeek)
                .collect(Collectors.toSet());
    }
}
