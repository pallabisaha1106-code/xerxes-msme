package com.xerxes.msme.admin.dto;

import java.util.UUID;

public record AdminServiceCityResponse(UUID id, String stateCode, String name, int displayOrder, boolean active) {}
