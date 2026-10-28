package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDefaultProductRequest(
        @NotNull UUID defaultProductCategoryId, @NotBlank String name, int displayOrder) {}
