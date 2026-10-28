package com.xerxes.msme.appointment.dto;

import java.util.List;

/**
 * What a shopper needs to request a slot: how long each appointment is, and the dates they can pick with the free
 * times on each. Dates with no free time are left out (an unpublished or fully booked date looks the same), so an
 * empty list means nothing can be booked right now.
 */
public record AvailabilityResponse(int slotMinutes, List<AvailableDayResponse> days) {}
