package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.partner.dto.PartnerContactResponse;
import java.util.UUID;

public record VendorApplicationResponse(
        UUID tenantId, String registrationCode, String businessName, PartnerContactResponse partner) {}
