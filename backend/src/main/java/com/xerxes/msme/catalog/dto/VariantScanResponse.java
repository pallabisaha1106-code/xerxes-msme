package com.xerxes.msme.catalog.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** What a barcode scan at the billing counter needs to add a line item without further lookups. */
public record VariantScanResponse(
        UUID productId,
        String productName,
        UUID variantId,
        String sku,
        String barcode,
        Map<String, String> attributes,
        BigDecimal sellingPrice,
        BigDecimal effectiveGstRate,
        String effectiveHsnCode,
        BigDecimal currentStock) {}
