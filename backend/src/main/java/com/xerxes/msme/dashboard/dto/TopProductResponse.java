package com.xerxes.msme.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductResponse(
        UUID variantId, String productName, String sku, BigDecimal quantitySold, BigDecimal revenue) {}
