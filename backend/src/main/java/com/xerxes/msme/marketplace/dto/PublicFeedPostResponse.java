package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.tenant.BusinessType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** What a shopper sees in the public feed - approved posts within the 14-day window only. */
public record PublicFeedPostResponse(
        UUID tenantId,
        String businessName,
        String shopPhotoUrl,
        String area,
        String categoryName,
        /** WHOLESALER or RETAILER - an account with no type is reported as a retailer. */
        BusinessType businessType,
        String enquiryWhatsapp,
        String content,
        String discountLabel,
        LocalDate validUntil,
        OffsetDateTime createdAt) {}
