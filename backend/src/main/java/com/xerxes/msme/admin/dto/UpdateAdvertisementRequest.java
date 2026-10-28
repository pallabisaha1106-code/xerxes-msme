package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record UpdateAdvertisementRequest(
        String linkUrl,
        @NotBlank String altText,
        String headline,
        String description,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt) {}
