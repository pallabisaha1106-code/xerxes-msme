package com.xerxes.msme.storefront.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StorefrontOrderLineItemResponse(
        UUID variantId,
        String productName,
        String sku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {}
