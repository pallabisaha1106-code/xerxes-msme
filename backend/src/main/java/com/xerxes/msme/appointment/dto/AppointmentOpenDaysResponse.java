package com.xerxes.msme.appointment.dto;

import java.util.List;

/** Every date in the booking window, earliest first, whether published or not. */
public record AppointmentOpenDaysResponse(List<OpenDayResponse> days) {}
