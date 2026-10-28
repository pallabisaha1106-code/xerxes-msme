package com.xerxes.msme.appointment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentSettingsRepository extends JpaRepository<AppointmentSettings, UUID> {

    Optional<AppointmentSettings> findByTenantId(UUID tenantId);
}
