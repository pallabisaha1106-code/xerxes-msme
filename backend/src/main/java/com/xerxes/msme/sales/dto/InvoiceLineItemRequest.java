package com.xerxes.msme.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineItemRequest(
        @NotNull UUID variantId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice) {}
