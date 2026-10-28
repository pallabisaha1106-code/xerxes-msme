package com.xerxes.msme.partner;

/** What a Super Admin or a partner (or the system) did to a shop - see ReviewAction. */
public enum ReviewActionType {
    ACCOUNT_ACTIVATED,
    ACCOUNT_SUSPENDED,
    LISTING_APPROVED,
    LISTING_REJECTED,
    LISTING_UNPUBLISHED,
    FEED_POST_APPROVED,
    FEED_POST_REJECTED,
    PARTNER_ASSIGNED,
    PARTNER_REASSIGNED
}
