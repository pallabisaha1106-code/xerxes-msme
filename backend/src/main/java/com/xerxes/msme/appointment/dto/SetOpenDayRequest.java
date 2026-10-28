package com.xerxes.msme.appointment.dto;

import jakarta.validation.constraints.NotNull;

/** Publish ({@code true}) or unpublish ({@code false}) one date. */
public record SetOpenDayRequest(@NotNull Boolean open) {}
