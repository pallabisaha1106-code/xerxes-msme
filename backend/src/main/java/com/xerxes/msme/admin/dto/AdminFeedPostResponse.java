package com.xerxes.msme.admin.dto;

import com.xerxes.msme.marketplace.FeedPostStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A feed post as seen by platform staff reviewing it - the vendor's own FeedPostResponse plus who they are. */
public record AdminFeedPostResponse(
        UUID id,
        UUID tenantId,
        String businessName,
        String content,
        String discountLabel,
        LocalDate validUntil,
        FeedPostStatus status,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt) {}
