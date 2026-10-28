package com.xerxes.msme.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String brand,
        String description,
        UUID categoryId,
        String defaultHsnCode,
        BigDecimal defaultGstRate,
        String defaultUom,
        String fabric,
        String careInstructions,
        String includedItems,
        List<VariantResponse> variants) {}
