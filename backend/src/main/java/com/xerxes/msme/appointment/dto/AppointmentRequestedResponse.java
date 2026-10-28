package com.xerxes.msme.appointment.dto;

import java.time.LocalDate;

/** What a shopper is told after requesting: the business confirms offline, so this is only a summary to show back. */
public record AppointmentRequestedResponse(String serviceName, LocalDate date, String time, int slotMinutes) {}
