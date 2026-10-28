package com.xerxes.msme.storefront.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** No price here - the price is always resolved server-side from the variant's current selling price. */
public record StorefrontOrderLineItemRequest(
        @NotNull UUID variantId, @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity) {}
