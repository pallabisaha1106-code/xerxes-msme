package com.xerxes.msme.admin.dto;

import java.util.UUID;

public record AdminServiceAreaResponse(
        UUID id, UUID cityId, String name, String pincode, int displayOrder, boolean active) {}
