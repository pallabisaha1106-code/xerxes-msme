package com.xerxes.msme.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicApiRateLimitInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void preHandle_withinLimit_allowsRequestAndLeavesResponseUntouched() throws Exception {
        PublicApiRateLimiter rateLimiter = mock(PublicApiRateLimiter.class);
        when(rateLimiter.tryAcquire("1.2.3.4")).thenReturn(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        PublicApiRateLimitInterceptor interceptor = new PublicApiRateLimitInterceptor(rateLimiter, objectMapper);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verifyNoInteractions(response);
    }

    @Test
    void preHandle_overLimit_returns429WithAnApiErrorBodyAndStopsTheChain() throws Exception {
        PublicApiRateLimiter rateLimiter = mock(PublicApiRateLimiter.class);
        when(rateLimiter.tryAcquire("1.2.3.4")).thenReturn(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getRequestURI()).thenReturn("/api/public/marketplace/categories");
        MockHttpServletResponse response = new MockHttpServletResponse();
        PublicApiRateLimitInterceptor interceptor = new PublicApiRateLimitInterceptor(rateLimiter, objectMapper);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        ApiError body = objectMapper.readValue(response.getContentAsString(), ApiError.class);
        assertThat(body.status()).isEqualTo(429);
        assertThat(body.message()).isEqualTo("Too many requests. Please try again shortly.");
        assertThat(body.path()).isEqualTo("/api/public/marketplace/categories");
    }
}
