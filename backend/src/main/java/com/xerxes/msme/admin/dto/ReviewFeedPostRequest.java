package com.xerxes.msme.admin.dto;

/** Mirrors ReviewMarketplaceListingRequest exactly - reason is shown to the vendor on rejection. */
public record ReviewFeedPostRequest(String reason) {}
