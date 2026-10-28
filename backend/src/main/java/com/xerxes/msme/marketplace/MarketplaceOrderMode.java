package com.xerxes.msme.marketplace;

/** How a vendor wants to receive marketplace orders - set per listing, not global. */
public enum MarketplaceOrderMode {
    /** A WhatsApp deep link with a pre-filled enquiry/cart message - no account, no real order. */
    WHATSAPP,
    /** A real order via the same public storefront pipeline used by that vendor's own generated site. */
    CHECKOUT
}
