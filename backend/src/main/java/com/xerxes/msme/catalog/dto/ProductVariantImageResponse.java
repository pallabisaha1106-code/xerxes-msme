package com.xerxes.msme.catalog.dto;

import java.util.UUID;

public record ProductVariantImageResponse(
        UUID id, String imageUrl, String thumbnailUrl, int width, int height, int displayOrder) {}
