package com.xerxes.msme.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void corsConfigurationSource_allowsConfiguredOrigin() {
        CorsConfigurationSource source =
                securityConfig.corsConfigurationSource(new CorsProperties(List.of("http://localhost:5173")));

        HttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/register");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSource_appliesToEveryPath() {
        CorsConfigurationSource source =
                securityConfig.corsConfigurationSource(new CorsProperties(List.of("http://localhost:5173")));

        HttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/products/123");
        assertThat(source.getCorsConfiguration(request)).isNotNull();
    }

    @Test
    void corsConfigurationSource_publicStorefrontApi_allowsAnyOriginWithoutCredentials() {
        CorsConfigurationSource source =
                securityConfig.corsConfigurationSource(new CorsProperties(List.of("http://localhost:5173")));

        HttpServletRequest request =
                new MockHttpServletRequest("OPTIONS", "/api/public/storefront/" + java.util.UUID.randomUUID() + "/orders");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns()).containsExactly("*");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    @Test
    void passwordEncoder_hashesAndVerifies() {
        var encoder = securityConfig.passwordEncoder();
        String hash = encoder.encode("SecurePass123");

        assertThat(hash).isNotEqualTo("SecurePass123");
        assertThat(encoder.matches("SecurePass123", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }
}
