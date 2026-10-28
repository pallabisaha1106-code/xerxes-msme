package com.xerxes.msme.marketplace;

import org.springframework.data.domain.Sort;

/**
 * How the public feed is ordered. The sort is handed to the repository through the Pageable (see
 * FeedPostRepository.findPublicFeedPage), so a new ordering is one more constant here, not another query.
 */
public enum FeedSort {
    /** Most recently posted first. */
    NEWEST(Sort.by(Sort.Order.desc("createdAt"))),

    /**
     * Offers with an end date first, the one closing soonest at the top; offers without one follow, newest
     * first. Posts with no end date just use the default 14-day window, so they have no real "ending" to rank.
     */
    ENDING_SOON(Sort.by(Sort.Order.asc("validUntil").nullsLast(), Sort.Order.desc("createdAt")));

    private final Sort sort;

    FeedSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
