package com.xerxes.msme.admin.dto;

import com.xerxes.msme.marketplace.AdvertisementSlot;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateAdvertisementRequest(
        @NotNull AdvertisementSlot slot,
        String linkUrl,
        @NotBlank String altText,
        String headline,
        String description,
        @NotNull OffsetDateTime startsAt,
        @NotNull @Future OffsetDateTime endsAt) {}
