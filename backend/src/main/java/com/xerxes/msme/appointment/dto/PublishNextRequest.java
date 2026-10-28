package com.xerxes.msme.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** How many days from today to publish (1 to 14, the whole booking window). */
public record PublishNextRequest(@NotNull @Min(1) @Max(14) Integer days) {}
