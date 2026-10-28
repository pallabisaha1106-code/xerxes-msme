package com.xerxes.msme.subscription;

import com.xerxes.msme.common.exception.AccountSuspendedException;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Hard-locks a tenant whose subscription is SUSPENDED out of the API (see
 * WebConfig for the excluded paths that stay reachable - /api/subscription
 * so they can still view/pay their outstanding invoice). Deliberately a
 * HandlerInterceptor, not part of JwtAuthenticationFilter: a Filter runs
 * before the DispatcherServlet, so an exception thrown there would bypass
 * GlobalExceptionHandler entirely (the same class of pitfall as the /error
 * forward issue) - an interceptor's preHandle runs inside the dispatch loop,
 * so AccountSuspendedException here is caught and mapped to 403 correctly.
 *
 * <p>Takes an ObjectProvider rather than TenantRepository directly: WebConfig
 * (which constructs this) is pulled into every @WebMvcTest slice via
 * WebMvcConfigurer, and those slices don't provide JPA repository beans. A
 * direct constructor dependency would break every existing controller test
 * unless each one added a @MockBean for it; resolving lazily here means a
 * slice with no TenantRepository bean just skips the check (fail-open),
 * while the real app - which always has one - behaves normally.
 */
public class TenantSubscriptionInterceptor implements HandlerInterceptor {

    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;

    public TenantSubscriptionInterceptor(ObjectProvider<TenantRepository> tenantRepositoryProvider) {
        this.tenantRepositoryProvider = tenantRepositoryProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            // No authenticated tenant on this request - nothing to gate (auth
            // itself, e.g. a missing/invalid token, is handled downstream).
            return true;
        }

        TenantRepository tenantRepository = tenantRepositoryProvider.getIfAvailable();
        if (tenantRepository == null) {
            return true;
        }

        Tenant tenant = tenantRepository.findById(principal.tenantId()).orElse(null);
        if (tenant == null || tenant.isPlatform()) {
            // A missing tenant is some other code path's problem to report;
            // the sentinel platform tenant (Super Admin) is never gated.
            return true;
        }

        if (tenant.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED) {
            throw new AccountSuspendedException(
                    "Your subscription payment is overdue. Please pay your outstanding invoice to continue.");
        }

        return true;
    }
}
