package com.xerxes.msme.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xerxes.msme.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthContextTest {

    private final AuthContext authContext = new AuthContext();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void current_returnsThePrincipalPlacedByTheJwtFilter() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), UUID.randomUUID(), "a@b.com", Role.STAFF, UUID.randomUUID());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));

        assertThat(authContext.current()).isEqualTo(principal);
    }

    @Test
    void current_withNonAuthenticatedPrincipalType_throwsIllegalState() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("not-our-principal", null));

        assertThrows(IllegalStateException.class, authContext::current);
    }
}
