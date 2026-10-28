package com.xerxes.msme.security;

import com.xerxes.msme.user.Role;
import java.util.UUID;

/** What we know about the caller once their JWT has been verified. tokenId is the JWT's own id (jti), used to revoke it on logout. */
public record AuthenticatedPrincipal(UUID userId, UUID tenantId, String email, Role role, UUID tokenId) {}
