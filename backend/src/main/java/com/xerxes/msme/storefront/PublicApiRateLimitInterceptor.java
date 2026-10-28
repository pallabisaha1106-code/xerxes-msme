package com.xerxes.msme.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

/** Not a {@code @Component} - see PublicApiRateLimiter's javadoc; WebConfig constructs and registers this itself. */
public class PublicApiRateLimitInterceptor implements HandlerInterceptor {

    private final PublicApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public PublicApiRateLimitInterceptor(PublicApiRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!rateLimiter.tryAcquire(request.getRemoteAddr())) {
            // Same ApiError JSON shape as everywhere else - this interceptor runs before
            // DispatcherServlet, so GlobalExceptionHandler can never see this rejection.
            ApiError.writeTo(
                    response,
                    objectMapper,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again shortly.",
                    request.getRequestURI());
            return false;
        }
        return true;
    }
}
