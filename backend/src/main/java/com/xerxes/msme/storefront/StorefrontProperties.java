package com.xerxes.msme.storefront;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The generated static site's JS calls back to this backend (order submission,
 * and this base URL baked into config.js) from whatever domain the vendor
 * hosts it on - it can't be same-origin, so it must be an absolute URL
 * configured per environment rather than inferred from the incoming request.
 *
 * stockCheckEnabled gates whether product stock is treated as a real
 * constraint at all: when false, every variant is reported "in stock" to
 * customers regardless of currentStock, and StockService stops rejecting
 * movements that would take a variant negative. Off for now - inventory
 * isn't managed through the portal yet (availability is checked manually) -
 * flip on once real stock tracking goes live.
 */
@ConfigurationProperties(prefix = "app.storefront")
public record StorefrontProperties(String publicApiBaseUrl, boolean stockCheckEnabled) {}
