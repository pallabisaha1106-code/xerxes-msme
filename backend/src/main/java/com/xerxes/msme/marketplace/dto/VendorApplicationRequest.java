package com.xerxes.msme.marketplace.dto;

import com.xerxes.msme.common.EmailFormat;
import com.xerxes.msme.tenant.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Step 1 of vendor registration - business/owner details, sent alongside 3
 * document parts. See VendorApplicationController. cityId/areaId are gated
 * on Super Admin's curated ServiceCity/ServiceArea list (see
 * AdminServiceLocationService) rather than free-typed - landmark is the one
 * free-text location field, since it's inherently unique per business.
 */
public record VendorApplicationRequest(
        @NotBlank String businessName,
        @NotNull BusinessType businessType,
        @NotNull UUID marketplaceCategoryId,
        String gstin,
        @NotBlank @Pattern(regexp = "^[0-9]{2}$", message = "stateCode must be a 2-digit GST state code")
                String stateCode,
        String addressLine1,
        @NotNull UUID cityId,
        @NotNull UUID areaId,
        String landmark,
        String phone,
        @NotBlank String ownerName,
        @NotBlank @Email(regexp = EmailFormat.REGEX, message = EmailFormat.MESSAGE) String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {}
