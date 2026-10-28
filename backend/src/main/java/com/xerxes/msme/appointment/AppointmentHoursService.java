package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentHoursResponse;
import com.xerxes.msme.appointment.dto.OpeningRangeRequest;
import com.xerxes.msme.appointment.dto.OpeningRangeResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A business's weekly opening hours: the template that, together with the dates it publishes, decides which slots
 * exist. The whole week is replaced at once, so a half-saved week can never be seen.
 */
@Service
public class AppointmentHoursService {

    private static final Comparator<OpeningRangeRequest> BY_DAY_THEN_OPENING =
            Comparator.comparing(OpeningRangeRequest::dayOfWeek).thenComparing(OpeningRangeRequest::opensAt);

    private final AppointmentHoursRepository hoursRepository;
    private final AppointmentSettingsService settingsService;

    public AppointmentHoursService(AppointmentHoursRepository hoursRepository, AppointmentSettingsService settingsService) {
        this.hoursRepository = hoursRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public AppointmentHoursResponse getWeek(UUID tenantId) {
        return new AppointmentHoursResponse(hoursRepository.findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId).stream()
                .map(hours -> new OpeningRangeResponse(hours.getDayOfWeek(), hours.getOpensAt(), hours.getClosesAt()))
                .toList());
    }

    @Transactional
    public AppointmentHoursResponse replaceWeek(UUID tenantId, List<OpeningRangeRequest> ranges) {
        List<OpeningRangeRequest> sorted = ranges.stream().sorted(BY_DAY_THEN_OPENING).toList();
        validate(sorted, settingsService.get(tenantId).slotMinutes());

        hoursRepository.deleteAllByTenantId(tenantId);
        hoursRepository.saveAll(sorted.stream().map(range -> toEntity(tenantId, range)).toList());
        return new AppointmentHoursResponse(sorted.stream()
                .map(range -> new OpeningRangeResponse(range.dayOfWeek(), range.opensAt(), range.closesAt()))
                .toList());
    }

    /** Ranges arrive sorted by day then opening time, so overlap is a check against the previous range of the same day. */
    private static void validate(List<OpeningRangeRequest> sorted, int slotMinutes) {
        OpeningRangeRequest previous = null;
        int rangesToday = 0;
        for (OpeningRangeRequest range : sorted) {
            String day = DayOfWeek.of(range.dayOfWeek()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            if (!range.opensAt().isBefore(range.closesAt())) {
                throw new BusinessRuleViolationException(day + ": the opening time must be before the closing time.");
            }
            if (Duration.between(range.opensAt(), range.closesAt()).toMinutes() < slotMinutes) {
                throw new BusinessRuleViolationException(
                        day + ": each opening range must be at least one appointment (" + slotMinutes + " minutes) long.");
            }
            boolean sameDay = previous != null && previous.dayOfWeek().equals(range.dayOfWeek());
            rangesToday = sameDay ? rangesToday + 1 : 1;
            if (rangesToday > AppointmentRules.MAX_RANGES_PER_DAY) {
                throw new BusinessRuleViolationException(
                        day + ": at most " + AppointmentRules.MAX_RANGES_PER_DAY + " opening ranges per day.");
            }
            if (sameDay && range.opensAt().isBefore(previous.closesAt())) {
                throw new BusinessRuleViolationException(day + ": opening ranges must not overlap.");
            }
            previous = range;
        }
    }

    private static AppointmentHours toEntity(UUID tenantId, OpeningRangeRequest range) {
        AppointmentHours hours = new AppointmentHours();
        hours.setTenantId(tenantId);
        hours.setDayOfWeek(range.dayOfWeek());
        hours.setOpensAt(range.opensAt());
        hours.setClosesAt(range.closesAt());
        return hours;
    }
}
