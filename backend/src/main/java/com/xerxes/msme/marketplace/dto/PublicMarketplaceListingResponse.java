package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import java.util.UUID;

/**
 * imageProfile tells the shop page how to show product photos (e.g. offer zoom for DETAIL shops); bookable says the
 * shop's category takes appointment requests (a salon, a clinic) instead of orders.
 */
public record PublicMarketplaceListingResponse(
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
        ImageProfile imageProfile,
        boolean bookable) {

    public PublicMarketplaceListingResponse(
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
            MarketplaceOrderMode orderMode) {
        this(tenantId, businessName, categoryId, categoryName, area, shopPhotoUrl, shopPhotoAlt, instagramUrl, facebookUrl,
                youtubeUrl, googleMapsUrl, whatsappUrl, websiteUrl, enquiryWhatsapp, orderMode, ImageProfile.COMPACT, false);
    }
}
