package com.xerxes.msme.admin.dto;

import com.xerxes.msme.marketplace.ImageProfile;
import jakarta.validation.constraints.NotNull;

public record UpdateMarketplaceCategoryImageProfileRequest(@NotNull ImageProfile imageProfile) {}
