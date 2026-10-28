package com.xerxes.msme.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseLineItemResponse(
        UUID variantId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal gstRate, BigDecimal lineTotal) {}
