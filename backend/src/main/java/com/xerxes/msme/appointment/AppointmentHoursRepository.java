package com.xerxes.msme.appointment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentHoursRepository extends JpaRepository<AppointmentHours, UUID> {

    List<AppointmentHours> findAllByTenantIdOrderByDayOfWeekAscOpensAtAsc(UUID tenantId);

    void deleteAllByTenantId(UUID tenantId);
}
