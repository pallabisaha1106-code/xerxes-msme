package com.xerxes.msme.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CategoryRequest(@NotBlank String name, UUID parentCategoryId) {}
