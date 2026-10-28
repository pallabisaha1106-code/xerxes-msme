package com.xerxes.msme.user.dto;

import com.xerxes.msme.user.Role;
import java.util.UUID;

public record AuthResponse(
        String token, UUID tenantId, UUID userId, String email, Role role, String businessName) {}
