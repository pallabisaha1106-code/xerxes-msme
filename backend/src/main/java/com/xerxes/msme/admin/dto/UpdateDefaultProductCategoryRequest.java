package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDefaultProductCategoryRequest(@NotBlank String name, int displayOrder, boolean active) {}
