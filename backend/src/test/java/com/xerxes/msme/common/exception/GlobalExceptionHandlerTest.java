package com.xerxes.msme.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private HttpServletRequest request;
    @Mock private MethodArgumentNotValidException methodArgumentNotValidException;
    @Mock private BindingResult bindingResult;
    @Mock private MethodArgumentTypeMismatchException typeMismatchException;
    @Mock private HttpMessageNotReadableException notReadableException;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/things");
    }

    @Test
    void resourceNotFound_mapsTo404() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(ResourceNotFoundException.of("Product", "abc-123"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("Product").contains("abc-123");
        assertThat(response.getBody().path()).isEqualTo("/api/things");
    }

    @Test
    void duplicateResource_mapsTo409() {
        ResponseEntity<ApiError> response =
                handler.handleDuplicate(new DuplicateResourceException("SKU exists"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("SKU exists");
    }

    @Test
    void invalidCredentials_mapsTo401() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException("bad login"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accountSuspended_mapsTo403() {
        ResponseEntity<ApiError> response =
                handler.handleAccountSuspended(new AccountSuspendedException("suspended"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void businessRuleViolation_mapsTo422() {
        ResponseEntity<ApiError> response =
                handler.handleBusinessRule(new BusinessRuleViolationException("insufficient stock"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void illegalArgument_mapsTo400() {
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(new IllegalArgumentException("bad input"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validationFailure_mapsTo400WithFieldErrors() {
        FieldError fieldError = new FieldError("request", "name", "must not be blank");
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> response = handler.handleValidation(methodArgumentNotValidException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors()).containsExactly("must not be blank");
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
    }

    @Test
    void typeMismatch_mapsTo400_notSwallowedByTheCatchAll() {
        when(typeMismatchException.getName()).thenReturn("id");
        doReturn(UUID.class).when(typeMismatchException).getRequiredType();

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(typeMismatchException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("id").contains("UUID");
    }

    @Test
    void unreadableBody_mapsTo400_notSwallowedByTheCatchAll() {
        ResponseEntity<ApiError> response = handler.handleUnreadableBody(notReadableException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingParameter_mapsTo400_notSwallowedByTheCatchAll() {
        ResponseEntity<ApiError> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("categoryId", "UUID"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("categoryId");
    }

    @Test
    void methodNotSupported_mapsTo405_notSwallowedByTheCatchAll() {
        ResponseEntity<ApiError> response =
                handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("DELETE"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void unexpectedException_mapsTo500WithoutLeakingTheRawMessage() {
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new NullPointerException("some.internal.field was null"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("some.internal.field");
        assertThat(response.getBody().path()).isEqualTo("/api/things");
    }
}
