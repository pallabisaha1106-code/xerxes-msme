package com.xerxes.msme.appointment.dto;

import java.util.List;

/** The weekly template, ordered by weekday then opening time. */
public record AppointmentHoursResponse(List<OpeningRangeResponse> ranges) {}
