package com.xerxes.msme.subscription;

/**
 * TRIALING: free trial period after signup, full access.
 * ACTIVE: paid and current.
 * PAST_DUE: current period ended, an invoice is outstanding, grace period running - still full access.
 * SUSPENDED: grace period expired unpaid - hard-locked (see TenantSubscriptionInterceptor).
 */
public enum SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    SUSPENDED
}
