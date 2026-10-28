package com.xerxes.msme.security;

import com.xerxes.msme.user.Role;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and revokes JWTs, tracking each one's id (jti) in the DB so a
 * logout can invalidate it before its natural expiry - a plain JWT can't be
 * revoked on its own, since anyone holding it stays "valid" until it
 * expires. JwtAuthenticationFilter checks isValid() on every request
 * alongside JwtService's own signature/expiry check.
 */
@Service
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthTokenService(AuthTokenRepository authTokenRepository, JwtService jwtService, JwtProperties jwtProperties) {
        this.authTokenRepository = authTokenRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String issue(UUID userId, UUID tenantId, String email, Role role) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(jwtProperties.expirationMinutes());

        AuthToken authToken = new AuthToken();
        authToken.setUserId(userId);
        authToken.setExpiresAt(expiresAt);
        UUID tokenId = authTokenRepository.save(authToken).getId();

        return jwtService.generateToken(userId, tenantId, email, role, tokenId, expiresAt.toInstant());
    }

    @Transactional(readOnly = true)
    public boolean isValid(UUID tokenId) {
        return authTokenRepository.existsByIdAndRevokedAtIsNull(tokenId);
    }

    /** Signs the user out everywhere: every token they still hold stops working at once. */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        authTokenRepository.revokeAllForUser(userId, OffsetDateTime.now());
    }

    @Transactional
    public void revoke(UUID tokenId) {
        authTokenRepository.findById(tokenId).ifPresent(authToken -> {
            if (authToken.getRevokedAt() == null) {
                authToken.setRevokedAt(OffsetDateTime.now());
                authTokenRepository.save(authToken);
            }
        });
    }
}
