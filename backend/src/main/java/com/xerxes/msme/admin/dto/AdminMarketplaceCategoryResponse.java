package com.xerxes.msme.admin.dto;

import com.xerxes.msme.marketplace.ImageProfile;
import java.util.UUID;

public record AdminMarketplaceCategoryResponse(
        UUID id, String name, String icon, int displayOrder, boolean active, ImageProfile imageProfile, boolean bookable) {}
