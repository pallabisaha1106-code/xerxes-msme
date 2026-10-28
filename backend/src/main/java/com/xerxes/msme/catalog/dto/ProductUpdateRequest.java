package com.xerxes.msme.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Product-level fields only; variants are managed through their own add/edit/delete endpoints. */
public record ProductUpdateRequest(
        @NotBlank String name,
        String brand,
        String description,
        UUID categoryId,
        String defaultHsnCode,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultGstRate,
        @NotBlank String defaultUom,
        String fabric,
        String careInstructions,
        String includedItems) {}
