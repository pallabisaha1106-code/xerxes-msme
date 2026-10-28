package com.xerxes.msme.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** One opening range, times written as "HH:mm". dayOfWeek is ISO: 1 = Monday ... 7 = Sunday. */
public record OpeningRangeRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime closesAt) {}
