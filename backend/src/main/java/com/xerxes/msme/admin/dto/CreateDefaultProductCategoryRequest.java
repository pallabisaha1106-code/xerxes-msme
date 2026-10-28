package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDefaultProductCategoryRequest(
        @NotNull UUID marketplaceCategoryId, @NotBlank String name, int displayOrder) {}
