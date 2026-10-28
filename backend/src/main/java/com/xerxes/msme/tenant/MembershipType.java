package com.xerxes.msme.tenant;

/**
 * A tenant's catalogue-size tier. Distinct from SubscriptionStatus (billing
 * state - trial/active/past-due/suspended) - this is which plan, that is
 * whether it's paid up.
 */
public enum MembershipType {
    FREE,
    PLUS,
    PREMIUM;

    public int productLimit() {
        return switch (this) {
            case FREE -> 10;
            case PLUS -> 100;
            case PREMIUM -> 500;
        };
    }

    /** Monthly subscription price, in paise (Razorpay's own unit) - FREE is billed ₹0. */
    public long monthlyAmountPaise() {
        return switch (this) {
            case FREE -> 0;
            case PLUS -> 19_900;
            case PREMIUM -> 99_900;
        };
    }
}
