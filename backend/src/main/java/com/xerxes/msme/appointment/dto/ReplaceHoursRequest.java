package com.xerxes.msme.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The whole weekly template at once; an empty list means "closed every day". The service checks the ranges' rules. */
public record ReplaceHoursRequest(@NotNull @Size(max = 28) List<@Valid OpeningRangeRequest> ranges) {}
