package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.marketplace.MarketplaceListingStatus;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketplaceListingResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String area,
        String shopPhotoUrl,
        String shopPhotoAlt,
        String instagramUrl,
        String facebookUrl,
        String youtubeUrl,
        String googleMapsUrl,
        String whatsappUrl,
        String websiteUrl,
        String enquiryWhatsapp,
        MarketplaceOrderMode orderMode,
        MarketplaceListingStatus status,
        String rejectionReason,
        OffsetDateTime submittedAt) {}
