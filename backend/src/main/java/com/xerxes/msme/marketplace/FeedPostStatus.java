package com.xerxes.msme.marketplace;

/**
 * PENDING: submitted, awaiting Super Admin review - never shown publicly.
 * APPROVED: visible in the public feed until it falls out of the 14-day window (see FeedPostService).
 * REJECTED: terminal - unlike a marketplace listing, a feed post isn't resubmitted; the vendor
 * posts a new one instead, since this is one-shot ephemeral content, not a long-lived draft.
 */
public enum FeedPostStatus {
    PENDING,
    APPROVED,
    REJECTED
}
