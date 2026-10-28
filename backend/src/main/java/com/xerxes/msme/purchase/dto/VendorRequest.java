package com.xerxes.msme.purchase.dto;

import jakarta.validation.constraints.NotBlank;

public record VendorRequest(
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
