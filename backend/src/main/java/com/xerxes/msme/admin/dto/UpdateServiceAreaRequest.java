package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateServiceAreaRequest(@NotBlank String name, @NotBlank String pincode, int displayOrder, boolean active) {}
