package com.xerxes.msme.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, List.of());
    }

    public static ApiError withFieldErrors(
            int status, String error, String message, String path, List<String> fieldErrors) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, fieldErrors);
    }

    /**
     * For the handful of places outside GlobalExceptionHandler's reach that still need this same
     * response shape - namely Spring Security's AuthenticationEntryPoint/AccessDeniedHandler
     * (SecurityConfig), which run before DispatcherServlet and so can't be caught by a
     * @RestControllerAdvice. Writes status + this same ApiError JSON directly to the response.
     */
    public static void writeTo(
            HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), of(status.value(), status.getReasonPhrase(), message, path));
    }
}
