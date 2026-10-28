package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.marketplace.FeedPostStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A vendor's own post, including pending/rejected ones only they can see - see FeedPostService. */
public record FeedPostResponse(
        UUID id,
        String content,
        String discountLabel,
        LocalDate validUntil,
        FeedPostStatus status,
        String rejectionReason,
        OffsetDateTime createdAt) {}
