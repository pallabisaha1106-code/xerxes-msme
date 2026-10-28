package com.xerxes.msme.appointment;

/**
 * PENDING waits for the business. APPROVED keeps the slot. REJECTED, CANCELLED and EXPIRED free it. EXPIRED is not
 * stored today: a PENDING request whose start time has passed is reported as EXPIRED when read.
 */
public enum AppointmentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
