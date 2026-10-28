package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDefaultProductRequest(@NotBlank String name, int displayOrder, boolean active) {}
