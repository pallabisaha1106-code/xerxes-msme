package com.xerxes.msme.admin.dto;

/** Used for both reject (pending -> draft) and unpublish (live -> draft) - reason is shown to the vendor either way. */
public record ReviewMarketplaceListingRequest(String reason) {}
