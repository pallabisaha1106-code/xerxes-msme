package com.xerxes.msme.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.storage.StorageProperties;
import com.xerxes.msme.storefront.PublicApiRateLimitInterceptor;
import com.xerxes.msme.storefront.PublicApiRateLimiter;
import com.xerxes.msme.subscription.TenantSubscriptionInterceptor;
import com.xerxes.msme.tenant.TenantRepository;
import java.nio.file.Path;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Maps /media/** onto the local storage root - today that root only ever
 * contains {tenantId}/static/ (the generated storefront preview, see
 * StorefrontSiteGenerator/StorefrontSettingsService.previewUrl), since
 * product/marketplace photos moved to a public GCS bucket (see
 * ImageStorageService) and are no longer under here at all. Kept unauthenticated
 * for the same reason as before: a plain &lt;img src&gt;/iframe can't attach a
 * Bearer token - see SecurityConfig.
 *
 * <p>Also registers the rate limit on /api/public/**, the request log, and
 * the subscription-suspension check. The limiter and its interceptor are
 * plain objects owned here, not separate {@code @Component}s - implementing
 * WebMvcConfigurer already pulls this class into every {@code @WebMvcTest}
 * slice, so anything it needs has to resolve there too (same reasoning for
 * why TenantSubscriptionInterceptor takes an ObjectProvider rather than
 * TenantRepository directly - see its own javadoc). {@code
 * @EnableConfigurationProperties} is repeated here (also declared in
 * PropertiesConfig) for the same reason, for StorageProperties; Spring Boot
 * no-ops the second registration in the full app context.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;
    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    private final ObjectMapper objectMapper;
    private final PublicApiRateLimiter publicApiRateLimiter = new PublicApiRateLimiter();

    // Off in application-local.yml - a real anti-abuse guard in every deployed environment
    // (including prod), but pointless friction against yourself on local dev/Playwright runs.
    @Value("${app.public-api.rate-limit-enabled:true}")
    private boolean rateLimitEnabled;

    public WebConfig(
            StorageProperties storageProperties,
            ObjectProvider<TenantRepository> tenantRepositoryProvider,
            ObjectMapper objectMapper) {
        this.storageProperties = storageProperties;
        this.tenantRepositoryProvider = tenantRepositoryProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(storageProperties.root()).toAbsolutePath().normalize();
        registry.addResourceHandler("/media/**").addResourceLocations("file:" + root + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled) {
            registry.addInterceptor(new PublicApiRateLimitInterceptor(publicApiRateLimiter, objectMapper))
                    .addPathPatterns("/api/public/**");
        }
        registry.addInterceptor(new RequestLoggingInterceptor()).addPathPatterns("/api/**");
        registry.addInterceptor(new TenantSubscriptionInterceptor(tenantRepositoryProvider))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**", "/api/auth/**", "/api/subscription/**", "/api/webhooks/**");
    }
}
