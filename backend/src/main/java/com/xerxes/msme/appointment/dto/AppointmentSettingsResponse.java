package com.xerxes.msme.appointment.dto;

import java.util.List;

/** allowedSlotMinutes is the list the business can choose from, so a screen needn't hard-code it. */
public record AppointmentSettingsResponse(int slotMinutes, List<Integer> allowedSlotMinutes) {}
