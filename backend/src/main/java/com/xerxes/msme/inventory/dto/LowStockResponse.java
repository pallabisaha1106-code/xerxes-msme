package com.xerxes.msme.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockResponse(
        UUID variantId, String sku, BigDecimal currentStock, BigDecimal reorderThreshold) {}
