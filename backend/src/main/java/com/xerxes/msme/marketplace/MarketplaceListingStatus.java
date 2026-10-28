package com.xerxes.msme.marketplace;

/**
 * DRAFT: vendor is still editing, never shown publicly.
 * PENDING: submitted, awaiting Super Admin review.
 * LIVE: approved and visible on the public marketplace.
 * A rejection or an unpublish both send a listing back to DRAFT - see
 * rejectionReason on MarketplaceListing for why.
 */
public enum MarketplaceListingStatus {
    DRAFT,
    PENDING,
    LIVE
}
