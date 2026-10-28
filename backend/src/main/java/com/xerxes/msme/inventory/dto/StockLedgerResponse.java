package com.xerxes.msme.inventory.dto;

import com.xerxes.msme.inventory.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockLedgerResponse(
        UUID id,
        UUID variantId,
        MovementType movementType,
        BigDecimal quantityChange,
        BigDecimal balanceAfter,
        String referenceType,
        UUID referenceId,
        String remarks,
        LocalDate movementDate) {}
