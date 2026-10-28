package com.xerxes.msme.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentOpenDayRepository extends JpaRepository<AppointmentOpenDay, UUID> {

    /** The published dates from {@code from} to {@code to}, both included. */
    List<AppointmentOpenDay> findAllByTenantIdAndDayBetween(UUID tenantId, LocalDate from, LocalDate to);

    boolean existsByTenantIdAndDay(UUID tenantId, LocalDate day);

    void deleteByTenantIdAndDay(UUID tenantId, LocalDate day);
}
