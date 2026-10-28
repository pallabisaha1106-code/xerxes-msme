package com.xerxes.msme.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RequestLoggingInterceptorTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private final RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_recordsStartTimeAndAllowsTheRequestThrough() {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verifyAttributeWasSet();
    }

    private void verifyAttributeWasSet() {
        org.mockito.Mockito.verify(request).setAttribute(org.mockito.ArgumentMatchers.eq("requestStartTimeMillis"), any(Long.class));
    }

    @Test
    void afterCompletion_unauthenticatedRequest_logsWithoutThrowing() {
        when(request.getAttribute("requestStartTimeMillis")).thenReturn(System.currentTimeMillis());
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/public/marketplace/categories");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, new Object(), null);
    }

    @Test
    void afterCompletion_authenticatedRequest_logsWithoutThrowing() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(UUID.randomUUID(), UUID.randomUUID(), "a@b.com", Role.OWNER, UUID.randomUUID());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
        when(request.getAttribute("requestStartTimeMillis")).thenReturn(System.currentTimeMillis());
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/products");
        when(response.getStatus()).thenReturn(201);

        interceptor.afterCompletion(request, response, new Object(), null);
    }

    @Test
    void afterCompletion_missingStartTimeAttribute_doesNotThrow() {
        when(request.getAttribute("requestStartTimeMillis")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/dashboard");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, new Object(), null);
    }
}
