package com.xerxes.msme.appointment.dto;

import com.xerxes.msme.appointment.AppointmentStatus;
import java.util.UUID;

public record AppointmentStatusResponse(UUID id, AppointmentStatus status) {}
