package com.xerxes.msme.storefront.dto;

import java.time.OffsetDateTime;

public record StorefrontPublishResponse(OffsetDateTime publishedAt, String previewUrl) {}
