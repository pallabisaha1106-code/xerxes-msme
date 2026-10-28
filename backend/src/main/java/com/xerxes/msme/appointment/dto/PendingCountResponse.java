package com.xerxes.msme.appointment.dto;

/** Waiting requests that have not expired - the number on the inbox badge. */
public record PendingCountResponse(long pending) {}
