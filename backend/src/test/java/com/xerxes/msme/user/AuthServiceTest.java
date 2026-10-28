package com.xerxes.msme.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.AccountSuspendedException;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.InvalidCredentialsException;
import com.xerxes.msme.security.AuthTokenService;
import com.xerxes.msme.subscription.SubscriptionProperties;
import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.dto.AuthResponse;
import com.xerxes.msme.user.dto.LoginRequest;
import com.xerxes.msme.user.dto.RegisterRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthTokenService authTokenService;
    private final SubscriptionProperties subscriptionProperties = new SubscriptionProperties(30, 7);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                tenantRepository, userRepository, passwordEncoder, authTokenService, subscriptionProperties);
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest(
                "Test Traders", "27AAAPL1234C1Z5", "27", "Addr 1", null, "Mumbai", "400001", "9999999999",
                "Ravi Kumar", "ravi@example.com", "SecurePass123");
    }

    @Test
    void register_newBusiness_createsTenantAndOwnerAndReturnsToken() {
        when(tenantRepository.existsByEmailIgnoreCase("ravi@example.com")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode("SecurePass123")).thenReturn("hashed-password");
        when(authTokenService.issue(any(), any(), eq("ravi@example.com"), eq(Role.OWNER)))
                .thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest());

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("ravi@example.com");
        assertThat(response.role()).isEqualTo(Role.OWNER);
        assertThat(response.businessName()).isEqualTo("Test Traders");
        assertThat(response.tenantId()).isNotNull();
        assertThat(response.userId()).isNotNull();
    }

    @Test
    void register_newBusiness_startsTrialSubscription() {
        when(tenantRepository.existsByEmailIgnoreCase("ravi@example.com")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(authTokenService.issue(any(), any(), any(), any())).thenReturn("jwt-token");

        authService.register(registerRequest());

        org.mockito.ArgumentCaptor<Tenant> captor = org.mockito.ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        Tenant saved = captor.getValue();
        assertThat(saved.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(saved.getTrialEndsAt()).isNotNull();
        assertThat(saved.getCurrentPeriodEnd()).isEqualTo(saved.getTrialEndsAt());
    }

    @Test
    void register_duplicateBusinessEmail_throwsAndNeverPersists() {
        when(tenantRepository.existsByEmailIgnoreCase("ravi@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest()));

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    private User userWithHash(String hash) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTenantId(UUID.randomUUID());
        user.setEmail("ravi@example.com");
        user.setPasswordHash(hash);
        user.setRole(Role.OWNER);
        return user;
    }

    @Test
    void login_correctPassword_returnsTokenAndUpdatesLastLogin() {
        User user = userWithHash("hashed-password");
        Tenant tenant = new Tenant();
        tenant.setId(user.getTenantId());
        tenant.setBusinessName("Test Traders");

        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("ravi@example.com")).thenReturn(List.of(user));
        when(passwordEncoder.matches("SecurePass123", "hashed-password")).thenReturn(true);
        when(tenantRepository.findById(user.getTenantId())).thenReturn(Optional.of(tenant));
        when(authTokenService.issue(user.getId(), user.getTenantId(), user.getEmail(), Role.OWNER))
                .thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ravi@example.com", "SecurePass123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.businessName()).isEqualTo("Test Traders");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void login_noAccountWithEmail_throwsInvalidCredentials() {
        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("nobody@example.com")).thenReturn(List.of());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "whatever")));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = userWithHash("hashed-password");
        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("ravi@example.com")).thenReturn(List.of(user));
        when(passwordEncoder.matches(anyString(), eq("hashed-password"))).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ravi@example.com", "wrong-password")));
    }

    @Test
    void login_ambiguousEmailAcrossTenants_picksTheCandidateWhosePasswordMatches() {
        User wrongTenantUser = userWithHash("hash-a");
        User correctTenantUser = userWithHash("hash-b");
        Tenant tenant = new Tenant();
        tenant.setId(correctTenantUser.getTenantId());
        tenant.setBusinessName("Correct Tenant");

        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("ravi@example.com"))
                .thenReturn(List.of(wrongTenantUser, correctTenantUser));
        when(passwordEncoder.matches("secret", "hash-a")).thenReturn(false);
        when(passwordEncoder.matches("secret", "hash-b")).thenReturn(true);
        when(tenantRepository.findById(correctTenantUser.getTenantId())).thenReturn(Optional.of(tenant));
        when(authTokenService.issue(any(), any(), any(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ravi@example.com", "secret"));

        assertThat(response.businessName()).isEqualTo("Correct Tenant");
        verify(userRepository).save(correctTenantUser);
        verify(userRepository, never()).save(wrongTenantUser);
    }

    @Test
    void login_suspendedTenant_throwsAccountSuspendedAndNeverIssuesToken() {
        User user = userWithHash("hashed-password");
        Tenant tenant = new Tenant();
        tenant.setId(user.getTenantId());
        tenant.setBusinessName("Test Traders");
        tenant.setActive(false);

        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("ravi@example.com")).thenReturn(List.of(user));
        when(passwordEncoder.matches("SecurePass123", "hashed-password")).thenReturn(true);
        when(tenantRepository.findById(user.getTenantId())).thenReturn(Optional.of(tenant));

        assertThrows(
                AccountSuspendedException.class,
                () -> authService.login(new LoginRequest("ravi@example.com", "SecurePass123")));

        verify(userRepository, never()).save(any());
        verify(authTokenService, never()).issue(any(), any(), any(), any());
    }

    @Test
    void login_suspendedSubscription_throwsAccountSuspendedAndNeverIssuesToken() {
        User user = userWithHash("hashed-password");
        Tenant tenant = new Tenant();
        tenant.setId(user.getTenantId());
        tenant.setBusinessName("Test Traders");
        tenant.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);

        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("ravi@example.com")).thenReturn(List.of(user));
        when(passwordEncoder.matches("SecurePass123", "hashed-password")).thenReturn(true);
        when(tenantRepository.findById(user.getTenantId())).thenReturn(Optional.of(tenant));

        assertThrows(
                AccountSuspendedException.class,
                () -> authService.login(new LoginRequest("ravi@example.com", "SecurePass123")));

        verify(userRepository, never()).save(any());
        verify(authTokenService, never()).issue(any(), any(), any(), any());
    }

    @Test
    void login_userReferencesMissingTenant_throwsIllegalState() {
        User user = userWithHash("hashed-password");
        when(userRepository.findAllByEmailIgnoreCaseAndActiveTrue("ravi@example.com")).thenReturn(List.of(user));
        when(passwordEncoder.matches("SecurePass123", "hashed-password")).thenReturn(true);
        when(tenantRepository.findById(user.getTenantId())).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> authService.login(new LoginRequest("ravi@example.com", "SecurePass123")));
    }

    @Test
    void logout_delegatesToAuthTokenServiceRevoke() {
        UUID tokenId = UUID.randomUUID();

        authService.logout(tokenId);

        verify(authTokenService).revoke(tokenId);
    }
}
