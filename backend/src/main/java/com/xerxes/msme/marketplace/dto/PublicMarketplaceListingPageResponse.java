package com.xerxes.msme.marketplace.dto;

import java.util.List;

/** hasMore drives "Load more" on the public marketplace - no total count, so no COUNT query per page. */
public record PublicMarketplaceListingPageResponse(List<PublicMarketplaceListingResponse> items, boolean hasMore) {}
