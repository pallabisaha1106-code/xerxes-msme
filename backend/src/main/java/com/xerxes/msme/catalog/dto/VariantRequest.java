package com.xerxes.msme.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record VariantRequest(
        @NotBlank String sku,
        String barcode,
        Map<String, String> attributes,
        @NotNull @DecimalMin("0") BigDecimal purchasePrice,
        @NotNull @DecimalMin("0") BigDecimal sellingPrice,
        String hsnCode,
        @DecimalMin("0") @DecimalMax("100") BigDecimal gstRate,
        String uom,
        @DecimalMin("0") BigDecimal reorderThreshold) {}
