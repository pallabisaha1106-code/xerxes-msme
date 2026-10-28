package com.xerxes.msme.appointment.dto;

import com.xerxes.msme.appointment.AppointmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One request in the vendor's inbox. date, time and endsAt are the business's local date and "HH:mm" times. status is
 * the effective one (a waiting request whose time has passed reads EXPIRED). For a waiting request, othersAtSameTime
 * counts other waiting requests overlapping it and timeAlreadyBooked says an approved appointment already holds the time.
 */
public record AppointmentRequestResponse(
        UUID id,
        String serviceName,
        BigDecimal price,
        String customerName,
        String customerPhone,
        String note,
        LocalDate date,
        String time,
        String endsAt,
        AppointmentStatus status,
        int othersAtSameTime,
        boolean timeAlreadyBooked,
        OffsetDateTime requestedAt) {}
