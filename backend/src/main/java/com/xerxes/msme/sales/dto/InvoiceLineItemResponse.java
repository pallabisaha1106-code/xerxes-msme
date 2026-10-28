package com.xerxes.msme.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineItemResponse(
        UUID variantId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal gstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal lineTotal) {}
