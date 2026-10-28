package com.xerxes.msme.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        UUID vendorId,
        String referenceNumber,
        LocalDate purchaseDate,
        BigDecimal subtotal,
        BigDecimal totalTax,
        BigDecimal totalAmount,
        List<PurchaseLineItemResponse> lineItems) {}
