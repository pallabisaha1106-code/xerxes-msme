package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A business's appointment settings: today just how long each appointment is. */
@Service
public class AppointmentSettingsService {

    private final AppointmentSettingsRepository settingsRepository;

    public AppointmentSettingsService(AppointmentSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /** A business that has never saved settings gets the default length; nothing is stored until it does. */
    @Transactional(readOnly = true)
    public AppointmentSettingsResponse get(UUID tenantId) {
        int slotMinutes = settingsRepository
                .findByTenantId(tenantId)
                .map(AppointmentSettings::getSlotMinutes)
                .orElse(AppointmentRules.DEFAULT_SLOT_MINUTES);
        return toResponse(slotMinutes);
    }

    @Transactional
    public AppointmentSettingsResponse update(UUID tenantId, int slotMinutes) {
        if (!AppointmentRules.isAllowedSlotLength(slotMinutes)) {
            throw new BusinessRuleViolationException(
                    "Appointment length must be one of " + AppointmentRules.SLOT_LENGTHS + " minutes.");
        }
        AppointmentSettings settings = settingsRepository.findByTenantId(tenantId).orElseGet(() -> {
            AppointmentSettings created = new AppointmentSettings();
            created.setTenantId(tenantId);
            return created;
        });
        settings.setSlotMinutes(slotMinutes);
        settingsRepository.save(settings);
        return toResponse(slotMinutes);
    }

    private static AppointmentSettingsResponse toResponse(int slotMinutes) {
        return new AppointmentSettingsResponse(slotMinutes, AppointmentRules.SLOT_LENGTHS);
    }
}
