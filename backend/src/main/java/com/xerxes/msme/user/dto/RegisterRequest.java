package com.xerxes.msme.user.dto;

import com.xerxes.msme.common.EmailFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String businessName,
        String gstin,
        @NotBlank @Pattern(regexp = "^[0-9]{2}$", message = "stateCode must be a 2-digit GST state code")
                String stateCode,
        String addressLine1,
        String addressLine2,
        String city,
        String pincode,
        String phone,
        @NotBlank String ownerName,
        @NotBlank @Email(regexp = EmailFormat.REGEX, message = EmailFormat.MESSAGE) String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password) {}
