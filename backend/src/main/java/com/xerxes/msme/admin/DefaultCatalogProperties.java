package com.xerxes.msme.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * minImageSize is the shortest side, in pixels, a starter-catalogue photo must have
 * to be accepted (0 = no check). Small stock photos get stretched and look blurry on
 * the shop page, so this is switched on once the catalogue's photos are replaced.
 */
@ConfigurationProperties(prefix = "app.default-catalog")
public record DefaultCatalogProperties(int minImageSize) {}
