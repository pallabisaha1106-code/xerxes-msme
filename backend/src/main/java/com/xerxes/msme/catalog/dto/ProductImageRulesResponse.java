package com.xerxes.msme.catalog.dto;

import com.xerxes.msme.marketplace.ImageProfile;

/** The photo limits that apply to the calling vendor's shop, so the UI can check a photo before uploading it. */
public record ProductImageRulesResponse(
        ImageProfile profile, int minWidth, int minHeight, double minAspectRatio, double maxAspectRatio) {}
