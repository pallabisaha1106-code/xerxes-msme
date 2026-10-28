package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentRequestResponse;
import com.xerxes.msme.appointment.dto.AppointmentStatusResponse;
import com.xerxes.msme.appointment.dto.PendingCountResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business's side of appointment requests: the inbox, and approving, rejecting or cancelling. Approving is where
 * a clash is caught - several people can have asked for the same time, but only one request can be approved for it.
 * A waiting request whose start time has passed is reported as EXPIRED; nothing rewrites it in the database.
 */
@Service
public class AppointmentInboxService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public AppointmentInboxService(AppointmentRepository appointmentRepository, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    /** Requests from the start of today onward, earliest first; {@code status} (effective status) narrows the list. */
    @Transactional(readOnly = true)
    public List<AppointmentRequestResponse> list(UUID tenantId, AppointmentStatus status) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime startOfToday = AppointmentRules.today(clock).atStartOfDay(AppointmentRules.ZONE).toOffsetDateTime();
        List<Appointment> rows = appointmentRepository.findAllByTenantIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(tenantId, startOfToday);
        return rows.stream()
                .filter(row -> status == null || effectiveStatus(row, now) == status)
                .map(row -> toResponse(row, rows, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public PendingCountResponse pendingCount(UUID tenantId) {
        return new PendingCountResponse(appointmentRepository.countByTenantIdAndStatusAndStartsAtGreaterThan(
                tenantId, AppointmentStatus.PENDING, OffsetDateTime.now(clock)));
    }

    @Transactional
    public AppointmentStatusResponse approve(UUID tenantId, UUID id) {
        Appointment appointment = requireWaiting(tenantId, id, "approved");
        boolean clash = appointmentRepository
                .findOverlapping(tenantId, AppointmentStatus.APPROVED, appointment.getStartsAt(), appointment.getEndsAt()).stream()
                .anyMatch(other -> !other.getId().equals(appointment.getId()));
        if (clash) {
            throw new BusinessRuleViolationException("This time is already booked. Reject this request instead.");
        }
        return decide(appointment, AppointmentStatus.APPROVED);
    }

    @Transactional
    public AppointmentStatusResponse reject(UUID tenantId, UUID id) {
        return decide(requireWaiting(tenantId, id, "rejected"), AppointmentStatus.REJECTED);
    }

    /** Only an approved appointment can be cancelled; doing so frees the time again. */
    @Transactional
    public AppointmentStatusResponse cancel(UUID tenantId, UUID id) {
        Appointment appointment = find(tenantId, id);
        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new BusinessRuleViolationException("Only an approved appointment can be cancelled.");
        }
        return decide(appointment, AppointmentStatus.CANCELLED);
    }

    private Appointment requireWaiting(UUID tenantId, UUID id, String verb) {
        Appointment appointment = find(tenantId, id);
        AppointmentStatus effective = effectiveStatus(appointment, OffsetDateTime.now(clock));
        if (effective == AppointmentStatus.EXPIRED) {
            throw new BusinessRuleViolationException("This request has expired.");
        }
        if (effective != AppointmentStatus.PENDING) {
            throw new BusinessRuleViolationException("Only a waiting request can be " + verb + ".");
        }
        return appointment;
    }

    private Appointment find(UUID tenantId, UUID id) {
        return appointmentRepository.findByIdAndTenantId(id, tenantId).orElseThrow(() -> ResourceNotFoundException.of("Appointment", id));
    }

    private AppointmentStatusResponse decide(Appointment appointment, AppointmentStatus status) {
        appointment.setStatus(status);
        appointment.setDecidedAt(OffsetDateTime.now(clock));
        appointmentRepository.save(appointment);
        return new AppointmentStatusResponse(appointment.getId(), status);
    }

    private static AppointmentStatus effectiveStatus(Appointment appointment, OffsetDateTime now) {
        return appointment.getStatus() == AppointmentStatus.PENDING && !appointment.getStartsAt().isAfter(now)
                ? AppointmentStatus.EXPIRED
                : appointment.getStatus();
    }

    private static boolean overlaps(Appointment a, Appointment b) {
        return a.getStartsAt().isBefore(b.getEndsAt()) && a.getEndsAt().isAfter(b.getStartsAt());
    }

    private static AppointmentRequestResponse toResponse(Appointment appointment, List<Appointment> all, OffsetDateTime now) {
        AppointmentStatus effective = effectiveStatus(appointment, now);
        int others = 0;
        boolean booked = false;
        if (effective == AppointmentStatus.PENDING) {
            for (Appointment other : all) {
                if (other == appointment || !overlaps(appointment, other)) continue;
                if (other.getStatus() == AppointmentStatus.APPROVED) booked = true;
                if (effectiveStatus(other, now) == AppointmentStatus.PENDING) others++;
            }
        }
        ZonedDateTime start = appointment.getStartsAt().atZoneSameInstant(AppointmentRules.ZONE);
        ZonedDateTime end = appointment.getEndsAt().atZoneSameInstant(AppointmentRules.ZONE);
        return new AppointmentRequestResponse(
                appointment.getId(),
                appointment.getServiceName(),
                appointment.getPrice(),
                appointment.getCustomerName(),
                appointment.getCustomerPhone(),
                appointment.getNote(),
                start.toLocalDate(),
                start.format(TIME),
                end.format(TIME),
                effective,
                others,
                booked,
                appointment.getCreatedAt());
    }
}
