package com.xerxes.msme.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public record OpeningRangeResponse(
        int dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
        @JsonFormat(pattern = "HH:mm") LocalTime closesAt) {}
