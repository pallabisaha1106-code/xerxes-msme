package com.xerxes.msme.partner.dto;

import java.util.UUID;

/** An area with shops that need a partner, and how many active partners could take them. */
public record WaitingAreaResponse(UUID areaId, String areaName, String cityName, long waitingShops, long activePartners) {}
