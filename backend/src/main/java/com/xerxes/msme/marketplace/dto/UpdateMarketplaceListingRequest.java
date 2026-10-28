package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * All fields except orderMode are optional - a vendor fills a draft in
 * incrementally, and required-field validation only happens at submit time
 * (see MarketplaceListingService.submit).
 */
public record UpdateMarketplaceListingRequest(
        UUID categoryId,
        String area,
        String shopPhotoAlt,
        String instagramUrl,
        String facebookUrl,
        String youtubeUrl,
        String googleMapsUrl,
        String whatsappUrl,
        String websiteUrl,
        String enquiryWhatsapp,
        @NotNull MarketplaceOrderMode orderMode) {}
