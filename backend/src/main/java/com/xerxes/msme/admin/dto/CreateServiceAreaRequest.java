package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateServiceAreaRequest(
        @NotNull UUID cityId, @NotBlank String name, @NotBlank String pincode, int displayOrder) {}
