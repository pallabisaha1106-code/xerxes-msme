package com.xerxes.msme.storefront.dto;

import com.xerxes.msme.storefront.StorefrontOrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StorefrontOrderResponse(
        UUID id,
        StorefrontOrderStatus status,
        String customerName,
        String customerPhone,
        String customerEmail,
        String addressLine1,
        String addressLine2,
        String city,
        String pincode,
        String stateCode,
        String notes,
        BigDecimal subtotal,
        String rejectionReason,
        UUID confirmedInvoiceId,
        OffsetDateTime createdAt,
        List<StorefrontOrderLineItemResponse> lineItems) {}
