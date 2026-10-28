package com.xerxes.msme.config;

import com.xerxes.msme.security.AuthenticatedPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * One INFO line per request: method, path, status, how long it took, and
 * (when authenticated) who called it. Every controller gets this for free -
 * nothing to add per-endpoint. Errors are logged with full context/stack
 * trace separately by GlobalExceptionHandler; this line is the plain
 * request/response audit trail.
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("com.xerxes.msme.RequestLog");
    private static final String START_TIME_ATTRIBUTE = "requestStartTimeMillis";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        long durationMillis = startTime instanceof Long start ? System.currentTimeMillis() - start : -1;

        String caller = currentCallerDescription();
        log.info(
                "{} {} -> {} ({}ms){}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMillis,
                caller.isEmpty() ? "" : " " + caller);
    }

    private String currentCallerDescription() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return "[tenant=" + principal.tenantId() + " user=" + principal.userId() + "]";
        }
        return "";
    }
}
