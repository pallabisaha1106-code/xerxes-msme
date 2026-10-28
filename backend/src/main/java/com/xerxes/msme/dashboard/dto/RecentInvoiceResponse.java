package com.xerxes.msme.dashboard.dto;

import com.xerxes.msme.sales.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecentInvoiceResponse(
        UUID id,
        String invoiceDisplayNumber,
        String customerName,
        LocalDate invoiceDate,
        BigDecimal totalAmount,
        InvoiceStatus status) {}
