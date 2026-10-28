package com.xerxes.msme.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
        @NotBlank String name,
        String brand,
        String description,
        UUID categoryId,
        String defaultHsnCode,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultGstRate,
        @NotBlank String defaultUom,
        String fabric,
        String careInstructions,
        String includedItems,
        @NotEmpty @Valid List<VariantRequest> variants) {}
