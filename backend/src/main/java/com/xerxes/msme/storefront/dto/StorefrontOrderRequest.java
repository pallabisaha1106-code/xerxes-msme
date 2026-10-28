package com.xerxes.msme.storefront.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StorefrontOrderRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        String customerEmail,
        String addressLine1,
        String addressLine2,
        String city,
        String pincode,
        String stateCode,
        String notes,
        @NotEmpty @Valid List<StorefrontOrderLineItemRequest> lineItems) {}
