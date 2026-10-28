package com.xerxes.msme.appointment.dto;

import jakarta.validation.constraints.NotNull;

/** Must be one of AppointmentRules.SLOT_LENGTHS; the service checks that. */
public record UpdateAppointmentSettingsRequest(@NotNull Integer slotMinutes) {}
