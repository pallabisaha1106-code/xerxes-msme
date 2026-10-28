package com.xerxes.msme.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiErrorTest {

    @Test
    void of_createsErrorWithEmptyFieldErrors() {
        ApiError error = ApiError.of(404, "Not Found", "Product not found", "/api/products/1");

        assertThat(error.status()).isEqualTo(404);
        assertThat(error.error()).isEqualTo("Not Found");
        assertThat(error.message()).isEqualTo("Product not found");
        assertThat(error.path()).isEqualTo("/api/products/1");
        assertThat(error.fieldErrors()).isEmpty();
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    void withFieldErrors_carriesTheGivenFieldErrors() {
        ApiError error = ApiError.withFieldErrors(
                400, "Bad Request", "Validation failed", "/api/products", List.of("name must not be blank"));

        assertThat(error.fieldErrors()).containsExactly("name must not be blank");
    }

    @Test
    void resourceNotFoundException_ofFactory_buildsReadableMessage() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Vendor", "abc-123");
        assertThat(ex.getMessage()).isEqualTo("Vendor not found: abc-123");
    }

    @Test
    void writeTo_writesTheSameApiErrorShapeAsGlobalExceptionHandler() throws Exception {
        // findAndRegisterModules() picks up jackson-datatype-jsr310 (already on the classpath via
        // spring-boot-starter-web) so OffsetDateTime round-trips the same way the real,
        // Spring-Boot-configured ObjectMapper bean handles it everywhere else in this API.
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiError.writeTo(response, objectMapper, HttpStatus.UNAUTHORIZED, "Authentication required", "/api/products");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        ApiError body = objectMapper.readValue(response.getContentAsString(), ApiError.class);
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).isEqualTo("Authentication required");
        assertThat(body.path()).isEqualTo("/api/products");
        assertThat(body.fieldErrors()).isEmpty();
    }
}
