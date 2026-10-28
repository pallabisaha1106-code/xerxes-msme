package com.xerxes.msme.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateServiceCityRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{2}$", message = "stateCode must be a 2-digit GST state code") String stateCode,
        @NotBlank String name,
        int displayOrder) {}
