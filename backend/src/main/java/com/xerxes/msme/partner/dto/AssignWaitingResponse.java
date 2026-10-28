package com.xerxes.msme.partner.dto;

/** Result of handing out an area's waiting shops: how many got a partner and how many still have none. */
public record AssignWaitingResponse(int assigned, long stillWaiting) {}
