package com.xerxes.msme.subscription.dto;

import java.time.OffsetDateTime;

public record PendingInvoiceResponse(long amountPaise, String paymentUrl, OffsetDateTime createdAt) {}
