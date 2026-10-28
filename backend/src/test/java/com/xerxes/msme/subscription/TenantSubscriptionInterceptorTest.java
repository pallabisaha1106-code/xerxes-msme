package com.xerxes.msme.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.AccountSuspendedException;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TenantSubscriptionInterceptorTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private TenantRepository tenantRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID tenantId) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }

    private TenantSubscriptionInterceptor interceptorWithRepository() {
        ObjectProvider<TenantRepository> provider = new SingleValueObjectProvider<>(tenantRepository);
        return new TenantSubscriptionInterceptor(provider);
    }

    @Test
    void preHandle_unauthenticatedRequest_passesThrough() {
        TenantSubscriptionInterceptor interceptor = interceptorWithRepository();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_platformTenant_isNeverGated() {
        UUID tenantId = UUID.randomUUID();
        authenticateAs(tenantId);
        Tenant platformTenant = new Tenant();
        platformTenant.setPlatform(true);
        platformTenant.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(platformTenant));

        assertThat(interceptorWithRepository().preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_suspendedTenant_throwsAccountSuspended() {
        UUID tenantId = UUID.randomUUID();
        authenticateAs(tenantId);
        Tenant tenant = new Tenant();
        tenant.setPlatform(false);
        tenant.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> interceptorWithRepository().preHandle(request, response, new Object()))
                .isInstanceOf(AccountSuspendedException.class);
    }

    @Test
    void preHandle_activeTenant_passesThrough() {
        UUID tenantId = UUID.randomUUID();
        authenticateAs(tenantId);
        Tenant tenant = new Tenant();
        tenant.setPlatform(false);
        tenant.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        tenant.setSubscriptionPlan(MembershipType.PREMIUM);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThat(interceptorWithRepository().preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_noTenantRepositoryAvailable_failsOpen() {
        authenticateAs(UUID.randomUUID());
        TenantSubscriptionInterceptor interceptor = new TenantSubscriptionInterceptor(new SingleValueObjectProvider<>(null));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    /** A minimal ObjectProvider stub - Mockito can't mock ObjectProvider's generic default methods cleanly. */
    private record SingleValueObjectProvider<T>(T value) implements ObjectProvider<T> {
        @Override
        public T getObject() {
            return value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }
    }
}
