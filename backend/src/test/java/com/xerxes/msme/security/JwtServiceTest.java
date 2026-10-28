package com.xerxes.msme.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xerxes.msme.user.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService serviceWithSecret(String secret) {
        return new JwtService(new JwtProperties(secret, 30));
    }

    @Test
    void generateThenParse_roundTripsAllClaimsIncludingTokenId() {
        JwtService jwtService = serviceWithSecret("K".repeat(64));
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        String token = jwtService.generateToken(
                userId, tenantId, "owner@example.com", Role.OWNER, tokenId, Instant.now().plus(30, ChronoUnit.MINUTES));
        AuthenticatedPrincipal principal = jwtService.parse(token);

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.tenantId()).isEqualTo(tenantId);
        assertThat(principal.email()).isEqualTo("owner@example.com");
        assertThat(principal.role()).isEqualTo(Role.OWNER);
        assertThat(principal.tokenId()).isEqualTo(tokenId);
    }

    @Test
    void expiredToken_failsToParse() {
        JwtService jwtService = serviceWithSecret("K".repeat(64));
        String token = jwtService.generateToken(
                UUID.randomUUID(), UUID.randomUUID(), "a@b.com", Role.STAFF, UUID.randomUUID(),
                Instant.now().minusSeconds(60));

        assertThrows(ExpiredJwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        // Same length as the signer's key (so both map to the same HMAC
        // algorithm strength) but different content, so this is a genuine
        // signature mismatch rather than a key-too-weak-for-the-algorithm case.
        JwtService signer = serviceWithSecret("K".repeat(64));
        JwtService verifier = serviceWithSecret("Z".repeat(64));
        String token = signer.generateToken(
                UUID.randomUUID(), UUID.randomUUID(), "a@b.com", Role.STAFF, UUID.randomUUID(),
                Instant.now().plus(30, ChronoUnit.MINUTES));

        assertThrows(SignatureException.class, () -> verifier.parse(token));
    }
}
