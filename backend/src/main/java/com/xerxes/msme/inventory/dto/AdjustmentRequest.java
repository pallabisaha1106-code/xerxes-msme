package com.xerxes.msme.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdjustmentRequest(@NotNull BigDecimal signedDelta, String remarks) {}
