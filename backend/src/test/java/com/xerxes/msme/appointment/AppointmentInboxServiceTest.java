package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.appointment.dto.AppointmentRequestResponse;
import com.xerxes.msme.appointment.dto.AppointmentStatusResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentInboxServiceTest {

    // Monday 19 Oct 2026, 10:00 in India.
    private static final Instant NOW = Instant.parse("2026-10-19T04:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Mock private AppointmentRepository appointmentRepository;

    private AppointmentInboxService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AppointmentInboxService(appointmentRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** An appointment on the given local India date and "HH:mm" start, 30 minutes long. */
    private Appointment appointment(AppointmentStatus status, LocalDate date, String time) {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setTenantId(tenantId);
        appointment.setServiceName("Haircut (Men)");
        appointment.setCustomerName("Asha");
        appointment.setCustomerPhone("9876543210");
        appointment.setStatus(status);
        OffsetDateTime start = LocalDateTime.of(date, java.time.LocalTime.parse(time)).atZone(AppointmentRules.ZONE).toOffsetDateTime();
        appointment.setStartsAt(start);
        appointment.setEndsAt(start.plusMinutes(30));
        return appointment;
    }

    private void inbox(Appointment... rows) {
        when(appointmentRepository.findAllByTenantIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(any(), any())).thenReturn(List.of(rows));
    }

    // ---- the inbox ----

    @Test
    void listing_readsFromTheStartOfTodayInBusinessTime() {
        inbox();

        service.list(tenantId, null);

        verify(appointmentRepository).findAllByTenantIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(
                tenantId, TODAY.atStartOfDay(AppointmentRules.ZONE).toOffsetDateTime());
    }

    @Test
    void listing_showsLocalDateAndTimes_andTheCustomerDetails() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"));

        AppointmentRequestResponse row = service.list(tenantId, null).get(0);

        assertThat(row.date()).isEqualTo(TODAY.plusDays(1));
        assertThat(row.time()).isEqualTo("11:00");
        assertThat(row.endsAt()).isEqualTo("11:30");
        assertThat(row.serviceName()).isEqualTo("Haircut (Men)");
        assertThat(row.customerPhone()).isEqualTo("9876543210");
        assertThat(row.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void aWaitingRequestWhoseTimeHasPassed_readsExpired_butAnApprovedOneStaysApproved() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY, "09:00"), appointment(AppointmentStatus.APPROVED, TODAY, "09:30"),
                appointment(AppointmentStatus.PENDING, TODAY, "10:30"));

        assertThat(service.list(tenantId, null)).extracting(AppointmentRequestResponse::status)
                .containsExactly(AppointmentStatus.EXPIRED, AppointmentStatus.APPROVED, AppointmentStatus.PENDING);
    }

    @Test
    void aWaitingRequestStartingExactlyNow_alreadyReadsExpired() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY, "10:00"));

        assertThat(service.list(tenantId, null).get(0).status()).isEqualTo(AppointmentStatus.EXPIRED);
    }

    @Test
    void theStatusFilterWorksOnTheEffectiveStatus() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY, "09:00"), appointment(AppointmentStatus.PENDING, TODAY, "12:00"),
                appointment(AppointmentStatus.APPROVED, TODAY, "13:00"));

        assertThat(service.list(tenantId, AppointmentStatus.EXPIRED)).hasSize(1);
        assertThat(service.list(tenantId, AppointmentStatus.PENDING)).hasSize(1);
        assertThat(service.list(tenantId, AppointmentStatus.APPROVED)).hasSize(1);
        assertThat(service.list(tenantId, AppointmentStatus.REJECTED)).isEmpty();
    }

    @Test
    void twoWaitingRequestsForTheSameTime_eachSeeTheOther() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"), appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"),
                appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "12:00"));

        assertThat(service.list(tenantId, null)).extracting(AppointmentRequestResponse::othersAtSameTime).containsExactly(1, 1, 0);
    }

    @Test
    void aWaitingRequestOverlappingAnApprovedAppointment_isFlaggedAsAlreadyBooked() {
        inbox(appointment(AppointmentStatus.APPROVED, TODAY.plusDays(1), "11:00"), appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"));

        List<AppointmentRequestResponse> rows = service.list(tenantId, null);

        assertThat(rows.get(1).timeAlreadyBooked()).isTrue();
        assertThat(rows.get(1).othersAtSameTime()).isZero(); // the approved one is not "waiting"
        assertThat(rows.get(0).timeAlreadyBooked()).isFalse(); // only waiting requests carry hints
    }

    @Test
    void requestsThatMerelyTouchDoNotCountAsAtTheSameTime() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"), appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:30"));

        assertThat(service.list(tenantId, null)).extracting(AppointmentRequestResponse::othersAtSameTime).containsExactly(0, 0);
    }

    @Test
    void anExpiredRequestIsNotCountedAsSomeoneElseWaiting() {
        inbox(appointment(AppointmentStatus.PENDING, TODAY, "09:45"), appointment(AppointmentStatus.PENDING, TODAY, "10:15"));

        List<AppointmentRequestResponse> rows = service.list(tenantId, null);

        assertThat(rows.get(0).status()).isEqualTo(AppointmentStatus.EXPIRED);
        assertThat(rows.get(1).othersAtSameTime()).isZero();
    }

    @Test
    void theBadgeCountsWaitingRequestsStillInTheFuture() {
        when(appointmentRepository.countByTenantIdAndStatusAndStartsAtGreaterThan(tenantId, AppointmentStatus.PENDING, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .thenReturn(4L);

        assertThat(service.pendingCount(tenantId).pending()).isEqualTo(4);
    }

    // ---- decisions ----

    private Appointment stored(Appointment appointment) {
        when(appointmentRepository.findByIdAndTenantId(appointment.getId(), tenantId)).thenReturn(Optional.of(appointment));
        return appointment;
    }

    @Test
    void approving_aWaitingRequest_approvesItAndStampsTheDecision() {
        Appointment appointment = stored(appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"));
        when(appointmentRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());

        AppointmentStatusResponse response = service.approve(tenantId, appointment.getId());

        assertThat(response.status()).isEqualTo(AppointmentStatus.APPROVED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.APPROVED);
        assertThat(appointment.getDecidedAt().toInstant()).isEqualTo(NOW);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void approving_whenAnotherAppointmentIsAlreadyApprovedForThatTime_isRefused() {
        Appointment appointment = stored(appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"));
        Appointment other = appointment(AppointmentStatus.APPROVED, TODAY.plusDays(1), "11:00");
        when(appointmentRepository.findOverlapping(tenantId, AppointmentStatus.APPROVED, appointment.getStartsAt(), appointment.getEndsAt()))
                .thenReturn(List.of(other));

        BusinessRuleViolationException error =
                assertThrows(BusinessRuleViolationException.class, () -> service.approve(tenantId, appointment.getId()));

        assertThat(error.getMessage()).contains("already booked");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void approving_anExpiredRequest_isRefused() {
        Appointment appointment = stored(appointment(AppointmentStatus.PENDING, TODAY, "09:00"));

        BusinessRuleViolationException error =
                assertThrows(BusinessRuleViolationException.class, () -> service.approve(tenantId, appointment.getId()));

        assertThat(error.getMessage()).contains("expired");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void approving_aRequestThatIsAlreadyDecided_isRefused() {
        for (AppointmentStatus decided : List.of(AppointmentStatus.APPROVED, AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED)) {
            Appointment appointment = stored(appointment(decided, TODAY.plusDays(1), "11:00"));
            BusinessRuleViolationException error =
                    assertThrows(BusinessRuleViolationException.class, () -> service.approve(tenantId, appointment.getId()));
            assertThat(error.getMessage()).contains("Only a waiting request can be approved");
        }
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void approving_anUnknownRequest_isNotFound() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.approve(tenantId, id));
    }

    @Test
    void rejecting_aWaitingRequest_rejectsIt() {
        Appointment appointment = stored(appointment(AppointmentStatus.PENDING, TODAY.plusDays(1), "11:00"));

        assertThat(service.reject(tenantId, appointment.getId()).status()).isEqualTo(AppointmentStatus.REJECTED);
        assertThat(appointment.getDecidedAt()).isNotNull();
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void rejecting_anExpiredOrAlreadyDecidedRequest_isRefused() {
        Appointment expired = stored(appointment(AppointmentStatus.PENDING, TODAY, "09:00"));
        assertThrows(BusinessRuleViolationException.class, () -> service.reject(tenantId, expired.getId()));
        Appointment approved = stored(appointment(AppointmentStatus.APPROVED, TODAY.plusDays(1), "11:00"));
        assertThrows(BusinessRuleViolationException.class, () -> service.reject(tenantId, approved.getId()));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancelling_anApprovedAppointment_cancelsIt() {
        Appointment appointment = stored(appointment(AppointmentStatus.APPROVED, TODAY.plusDays(1), "11:00"));

        assertThat(service.cancel(tenantId, appointment.getId()).status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void cancelling_anythingButAnApprovedAppointment_isRefused() {
        for (AppointmentStatus status : List.of(AppointmentStatus.PENDING, AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED)) {
            Appointment appointment = stored(appointment(status, TODAY.plusDays(1), "11:00"));
            assertThrows(BusinessRuleViolationException.class, () -> service.cancel(tenantId, appointment.getId()));
        }
        verify(appointmentRepository, never()).save(any());
    }
}
