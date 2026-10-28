package com.xerxes.msme.appointment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndTenantId(UUID id, UUID tenantId);

    /** The vendor's inbox: everything starting at or after {@code from}, earliest first. */
    List<Appointment> findAllByTenantIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(UUID tenantId, OffsetDateTime from);

    /** Requests of one status that start after {@code after} - used to count a phone's waiting requests and the badge. */
    long countByTenantIdAndCustomerPhoneAndStatusAndStartsAtGreaterThan(
            UUID tenantId, String customerPhone, AppointmentStatus status, OffsetDateTime after);

    long countByTenantIdAndStatusAndStartsAtGreaterThan(UUID tenantId, AppointmentStatus status, OffsetDateTime after);

    /** Appointments of the given status whose time overlaps [start, end) (touching ends do not overlap). */
    @Query("select a from Appointment a where a.tenantId = :tenantId and a.status = :status"
            + " and a.startsAt < :end and a.endsAt > :start")
    List<Appointment> findOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("status") AppointmentStatus status,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    /** How many of this phone's pending or approved requests overlap [start, end). */
    @Query("select count(a) from Appointment a where a.tenantId = :tenantId and a.customerPhone = :phone"
            + " and a.status in (com.xerxes.msme.appointment.AppointmentStatus.PENDING,"
            + " com.xerxes.msme.appointment.AppointmentStatus.APPROVED)"
            + " and a.startsAt < :end and a.endsAt > :start")
    long countLiveOverlappingForPhone(
            @Param("tenantId") UUID tenantId,
            @Param("phone") String phone,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}
