package com.xerxes.msme.appointment.dto;

import java.time.LocalDate;
import java.util.List;

/** One published date and its free start times, written "HH:mm" and in order. Always at least one time. */
public record AvailableDayResponse(LocalDate date, List<String> times) {}
