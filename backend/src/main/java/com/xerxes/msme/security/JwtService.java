package com.xerxes.msme.security;

import com.xerxes.msme.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Pure JWT signing/parsing - no DB, no notion of expiration policy. The
 * token's id (jti) and expiration are supplied by the caller (see
 * AuthTokenService) so the signed claim always matches the DB row tracking
 * that same token for revocation.
 */
@Service
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, UUID tenantId, String email, Role role, UUID tokenId, Instant expiresAt) {
        return Jwts.builder()
                .id(tokenId.toString())
                .subject(userId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public AuthenticatedPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedPrincipal(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class)),
                claims.get(CLAIM_EMAIL, String.class),
                Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                UUID.fromString(claims.getId()));
    }
}
