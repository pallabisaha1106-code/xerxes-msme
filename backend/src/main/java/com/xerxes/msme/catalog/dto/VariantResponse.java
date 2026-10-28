package com.xerxes.msme.catalog.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String sku,
        String barcode,
        Map<String, String> attributes,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        String hsnCode,
        BigDecimal gstRate,
        String uom,
        String effectiveHsnCode,
        BigDecimal effectiveGstRate,
        String effectiveUom,
        BigDecimal currentStock,
        BigDecimal reorderThreshold,
        boolean active) {}
