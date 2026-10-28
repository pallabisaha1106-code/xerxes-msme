package com.xerxes.msme.sales.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String name,
        String gstin,
        String stateCode,
        String contactPerson,
        String phone,
        String email,
        String addressLine1,
        String addressLine2,
        String city,
        String pincode) {}
