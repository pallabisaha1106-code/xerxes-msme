package com.xerxes.msme.admin.dto;

import com.xerxes.msme.marketplace.AdvertisementSlot;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminAdvertisementResponse(
        UUID id,
        AdvertisementSlot slot,
        String imageUrl,
        String linkUrl,
        String altText,
        String headline,
        String description,
        boolean published,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        /** published=true AND now() is within [startsAt, endsAt] - what a visitor would actually see right now. */
        boolean currentlyLive) {}
