package com.xerxes.msme.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.ApiError;
import com.xerxes.msme.security.AuthTokenService;
import com.xerxes.msme.security.JwtAuthenticationFilter;
import com.xerxes.msme.security.JwtService;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Registered first: UrlBasedCorsConfigurationSource matches patterns in
        // registration order, so this more specific pattern must be checked
        // before the catch-all below or it would never be reached.
        //
        // The generated storefront zip's JS calls this from whatever domain the
        // vendor ends up hosting it on - an origin we can't know in advance, so
        // it can't go on the fixed allow-list. That's an acceptable trade-off
        // only because these endpoints are unauthenticated, cookie-free (no
        // allowCredentials) and rate-limited - see PublicApiRateLimitInterceptor.
        CorsConfiguration openForPublicApi = new CorsConfiguration();
        openForPublicApi.setAllowedOriginPatterns(List.of("*"));
        openForPublicApi.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        openForPublicApi.setAllowedHeaders(List.of("*"));
        openForPublicApi.setAllowCredentials(false);
        openForPublicApi.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/public/**", openForPublicApi);

        CorsConfiguration restricted = new CorsConfiguration();
        restricted.setAllowedOrigins(corsProperties.allowedOrigins());
        restricted.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        restricted.setAllowedHeaders(List.of("*"));
        restricted.setAllowCredentials(true);
        restricted.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", restricted);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            AuthTokenService authTokenService,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper objectMapper)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Default is DENY, which blocks the admin app from embedding a
                // tenant's generated /media/**/static/index.html in an <iframe>
                // preview. SAMEORIGIN keeps clickjacking protection against every
                // other origin while allowing that same-origin preview.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Explicit on both so there's no ambiguity: no/expired/revoked token -> 401
                // (frontend clears the session and redirects to login), authenticated but
                // wrong role (e.g. OWNER hitting /api/admin/**) -> 403 (session stays put).
                // Written as the same ApiError JSON shape as GlobalExceptionHandler - these two
                // run before DispatcherServlet, so a @RestControllerAdvice can never catch them;
                // without this they'd silently fall back to Spring Boot's own, differently-shaped
                // default error body instead of this API's consistent one.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> ApiError.writeTo(
                                response, objectMapper, HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) -> ApiError.writeTo(
                                response, objectMapper, HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI())))
                .authorizeHttpRequests(auth -> auth
                        // The two handlers above write their own response directly rather than
                        // calling sendError(), so they no longer forward here - but Spring Boot's
                        // own default error handling (e.g. a truly unmapped route) still can, and
                        // that forward re-enters this same filter chain as a fresh (anonymous)
                        // request. Without this, that second pass fails .anyRequest().authenticated()
                        // and rewrites the original status into a 401/403 instead of the real one.
                        .requestMatchers("/error").permitAll()
                        // Only register/login are anonymous - logout needs a valid (and
                        // still-unrevoked) token to know which one to revoke, so it falls
                        // through to the authenticated() catch-all below instead.
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // Serves the generated storefront preview (StorefrontSettingsService.previewUrl) -
                        // product/marketplace photos moved to a public GCS bucket (see ImageStorageService)
                        // and are no longer under here. No confidentiality requirement either way, and a
                        // plain <img>/iframe can't attach the Bearer token this API otherwise requires.
                        .requestMatchers("/media/**").permitAll()
                        // Swagger UI / OpenAPI spec - documentation only, no data behind these paths.
                        // Disable via springdoc.api-docs.enabled/springdoc.swagger-ui.enabled=false
                        // (see application.yml) before exposing this backend publicly in production.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        // The guest storefront order flow - no login exists for a storefront visitor.
                        // Rate-limited via PublicApiRateLimitInterceptor (see WebConfig).
                        .requestMatchers("/api/public/**").permitAll()
                        // Razorpay calls this server-to-server, never a browser - authenticated by
                        // its own HMAC signature (RazorpayClient.verifyWebhookSignature), not a
                        // Spring Security role. Deliberately not under /api/public/**, since that
                        // bucket's wide-open CORS policy is meaningless (and undesirable to imply)
                        // for a server-to-server callback.
                        .requestMatchers("/api/webhooks/**").permitAll()
                        // Cross-tenant vendor directory/drill-down for platform staff only - never
                        // reachable by an ordinary OWNER/STAFF token. See Role.SUPER_ADMIN.
                        // ...except the review screens a local partner shares with Super Admin: shops (read and
                        // activate/suspend only - plan, subscription, invoices and shop data stay Super Admin's),
                        // marketplace listings and feed posts. Each service narrows a partner to their own shops.
                        .requestMatchers(HttpMethod.GET, "/api/admin/tenants", "/api/admin/tenants/*")
                        .hasAnyRole("SUPER_ADMIN", "PARTNER")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/tenants/*/status")
                        .hasAnyRole("SUPER_ADMIN", "PARTNER")
                        .requestMatchers("/api/admin/marketplace/listings/**", "/api/admin/feed-posts/**")
                        .hasAnyRole("SUPER_ADMIN", "PARTNER")
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        // A local partner's own endpoints: signed in as a PARTNER, nobody else.
                        .requestMatchers("/api/partner/**").hasRole("PARTNER")
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, authTokenService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
