package com.xerxes.msme.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A shopper's request for a slot. variantId is the service (a product variant of the shop), date and time are the
 * business's local date and a start time from its availability list ("HH:mm"). The phone is checked by the service.
 */
public record CreateAppointmentRequest(
        @NotNull UUID variantId,
        @NotNull LocalDate date,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime time,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 200) String note) {}
