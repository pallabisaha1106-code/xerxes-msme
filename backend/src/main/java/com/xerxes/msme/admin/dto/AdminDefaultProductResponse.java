package com.xerxes.msme.admin.dto;

import java.util.UUID;

public record AdminDefaultProductResponse(
        UUID id, UUID defaultProductCategoryId, String name, int displayOrder, boolean active, String imageUrl) {}
