package com.xerxes.msme.sales.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String gstin,
        String stateCode,
        String contactPerson,
        String phone,
        String email,
        String addressLine1,
        String addressLine2,
        String city,
        String pincode) {}
