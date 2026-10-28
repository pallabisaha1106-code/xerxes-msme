package com.xerxes.msme.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.user.Role;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock private AuthTokenRepository authTokenRepository;
    @Mock private JwtService jwtService;

    private AuthTokenService authTokenService;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(authTokenRepository, jwtService, new JwtProperties("secret", 30));
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    void issue_savesTokenRowThenSignsJwtWithItsIdAndMatchingExpiry() {
        UUID tokenId = UUID.randomUUID();
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(inv -> {
            AuthToken t = inv.getArgument(0);
            t.setId(tokenId);
            return t;
        });
        when(jwtService.generateToken(eq(userId), eq(tenantId), eq("a@b.com"), eq(Role.OWNER), eq(tokenId), any()))
                .thenReturn("signed-jwt");

        String result = authTokenService.issue(userId, tenantId, "a@b.com", Role.OWNER);

        assertThat(result).isEqualTo("signed-jwt");

        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
        assertThat(captor.getValue().getRevokedAt()).isNull();
    }

    @Test
    void isValid_notRevoked_returnsTrue() {
        UUID tokenId = UUID.randomUUID();
        when(authTokenRepository.existsByIdAndRevokedAtIsNull(tokenId)).thenReturn(true);

        assertThat(authTokenService.isValid(tokenId)).isTrue();
    }

    @Test
    void isValid_revokedOrUnknown_returnsFalse() {
        UUID tokenId = UUID.randomUUID();
        when(authTokenRepository.existsByIdAndRevokedAtIsNull(tokenId)).thenReturn(false);

        assertThat(authTokenService.isValid(tokenId)).isFalse();
    }

    @Test
    void revoke_existingUnrevokedToken_stampsRevokedAt() {
        UUID tokenId = UUID.randomUUID();
        AuthToken token = new AuthToken();
        token.setId(tokenId);
        when(authTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        authTokenService.revoke(tokenId);

        assertThat(token.getRevokedAt()).isNotNull();
        verify(authTokenRepository).save(token);
    }

    @Test
    void revoke_alreadyRevoked_isIdempotentAndDoesNotResave() {
        UUID tokenId = UUID.randomUUID();
        AuthToken token = new AuthToken();
        token.setId(tokenId);
        OffsetDateTime firstRevocation = OffsetDateTime.now().minusMinutes(5);
        token.setRevokedAt(firstRevocation);
        when(authTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        authTokenService.revoke(tokenId);

        assertThat(token.getRevokedAt()).isEqualTo(firstRevocation);
        verify(authTokenRepository, never()).save(any());
    }

    @Test
    void revoke_unknownToken_doesNothing() {
        UUID tokenId = UUID.randomUUID();
        when(authTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        authTokenService.revoke(tokenId);

        verify(authTokenRepository, never()).save(any());
    }
}
