package com.xerxes.msme.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload limits for vendor product photos, by the shop category's image profile (see ImageProfile).
 * The plain limits apply to DETAIL shops (apparel etc. - portrait, sharp) and to shops whose profile
 * can't be determined; the compact ones apply to COMPACT shops (grocery pack shots).
 * Aspect ratio is width / height (portrait photos are below 1).
 */
@ConfigurationProperties(prefix = "app.product-images")
public record ProductImageProperties(
        int minWidth,
        int minHeight,
        double minAspectRatio,
        double maxAspectRatio,
        int compactMinWidth,
        int compactMinHeight,
        double compactMinAspectRatio,
        double compactMaxAspectRatio) {}
