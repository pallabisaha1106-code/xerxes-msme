package com.xerxes.msme.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateStarterProductsRequest(@NotNull List<UUID> defaultProductIds) {}
