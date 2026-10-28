package com.xerxes.msme.appointment.dto;

import java.time.LocalDate;

/** One date in the booking window: whether the business has published it, and whether its weekday has any hours. */
public record OpenDayResponse(LocalDate date, boolean open, boolean weekdayHasHours) {}
