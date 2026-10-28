package com.xerxes.msme.sales.dto;

import com.xerxes.msme.sales.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID customerId,
        String invoiceDisplayNumber,
        LocalDate invoiceDate,
        String placeOfSupplyStateCode,
        BigDecimal subtotal,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount,
        InvoiceStatus status,
        List<InvoiceLineItemResponse> lineItems) {}
