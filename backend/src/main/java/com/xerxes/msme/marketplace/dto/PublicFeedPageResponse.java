package com.xerxes.msme.marketplace.dto;

import java.util.List;

/** hasMore drives "Load more" on the public feed - no total count, so no COUNT query per page. */
public record PublicFeedPageResponse(List<PublicFeedPostResponse> items, boolean hasMore) {}
