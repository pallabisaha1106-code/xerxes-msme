package com.xerxes.msme.storefront.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record StorefrontSettingsRequest(@NotBlank String templateId, @NotNull Map<String, String> content) {}
