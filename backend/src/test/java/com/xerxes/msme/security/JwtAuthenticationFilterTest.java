package com.xerxes.msme.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private AuthTokenService authTokenService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, authTokenService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validAndNotRevokedBearerToken_populatesSecurityContextWithPrincipalAndRoleAuthority() throws Exception {
        UUID tokenId = UUID.randomUUID();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), UUID.randomUUID(), "a@b.com", Role.OWNER, tokenId);
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtService.parse("good-token")).thenReturn(principal);
        when(authTokenService.isValid(tokenId)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(principal);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OWNER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void revokedToken_clearsContextAndStillContinuesChain() throws Exception {
        UUID tokenId = UUID.randomUUID();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), UUID.randomUUID(), "a@b.com", Role.OWNER, tokenId);
        when(request.getHeader("Authorization")).thenReturn("Bearer logged-out-token");
        when(jwtService.parse("logged-out-token")).thenReturn(principal);
        when(authTokenService.isValid(tokenId)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_clearsContextAndStillContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.parse("bad-token")).thenThrow(new RuntimeException("boom"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_leavesContextEmptyAndContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtService, org.mockito.Mockito.never()).parse(any());
    }

    @Test
    void nonBearerAuthorizationHeader_isIgnored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
