package com.xerxes.msme.partner.dto;

import com.xerxes.msme.common.EmailFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Someone asking to become a local partner, with the areas they would like to cover. */
public record PartnerApplicationRequest(
        @NotBlank @Size(max = 120) String name,
        /** An Indian mobile number as typed; checked and stored in country-code form by the service. */
        @NotBlank String whatsapp,
        @NotBlank @Email(regexp = EmailFormat.REGEX, message = EmailFormat.MESSAGE) String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        @NotEmpty List<@NotNull UUID> areaIds) {}
