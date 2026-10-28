package com.xerxes.msme.admin.dto;

import java.util.UUID;

public record AdminDefaultProductCategoryResponse(
        UUID id, UUID marketplaceCategoryId, String name, int displayOrder, boolean active) {}
