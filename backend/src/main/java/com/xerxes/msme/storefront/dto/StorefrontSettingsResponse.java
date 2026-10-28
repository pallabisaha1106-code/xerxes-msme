package com.xerxes.msme.storefront.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** content is always fully populated - manifest defaults and tenant-derived defaults filled in for anything unset. */
public record StorefrontSettingsResponse(
        String templateId, Map<String, String> content, OffsetDateTime publishedAt, String previewUrl) {}
