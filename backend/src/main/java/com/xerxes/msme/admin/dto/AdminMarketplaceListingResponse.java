package com.xerxes.msme.admin.dto;

import com.xerxes.msme.marketplace.MarketplaceListingStatus;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A vendor's marketplace listing as seen by platform staff - the vendor's own MarketplaceListingResponse plus who they are. */
public record AdminMarketplaceListingResponse(
        UUID tenantId,
        String businessName,
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
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt) {}
