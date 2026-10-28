package com.xerxes.msme.user;

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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final SubscriptionProperties subscriptionProperties;

    public AuthService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            SubscriptionProperties subscriptionProperties) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.subscriptionProperties = subscriptionProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (tenantRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A business is already registered with email " + request.email());
        }

        Tenant tenant = new Tenant();
        tenant.setBusinessName(request.businessName());
        tenant.setGstin(request.gstin());
        tenant.setStateCode(request.stateCode());
        tenant.setAddressLine1(request.addressLine1());
        tenant.setAddressLine2(request.addressLine2());
        tenant.setCity(request.city());
        tenant.setPincode(request.pincode());
        tenant.setPhone(request.phone());
        tenant.setEmail(request.email());
        OffsetDateTime trialEndsAt = OffsetDateTime.now().plusDays(subscriptionProperties.trialDays());
        tenant.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        tenant.setTrialEndsAt(trialEndsAt);
        tenant.setCurrentPeriodEnd(trialEndsAt);
        tenant = tenantRepository.save(tenant);

        User owner = new User();
        owner.setTenantId(tenant.getId());
        owner.setName(request.ownerName());
        owner.setEmail(request.email());
        owner.setPasswordHash(passwordEncoder.encode(request.password()));
        owner.setRole(Role.OWNER);
        owner = userRepository.save(owner);

        String token = authTokenService.issue(owner.getId(), tenant.getId(), owner.getEmail(), owner.getRole());
        return new AuthResponse(
                token, tenant.getId(), owner.getId(), owner.getEmail(), owner.getRole(), tenant.getBusinessName());
    }

    // Deliberately NOT @Transactional: this method's two writes - the
    // last-login UPDATE and AuthTokenService.issue()'s auth_tokens INSERT -
    // both need a lock on this same user row (the INSERT via its FK to
    // users). Concurrent logins as the same user (two tabs, a flaky submit)
    // would then each hold the INSERT's shared FK lock while waiting to
    // upgrade to the UPDATE's exclusive lock, which is a textbook Postgres
    // deadlock (each waits for the other's whole transaction to finish).
    // Leaving each write to commit in its own transaction (Spring Data's
    // default per-call transaction, and AuthTokenService.issue()'s own
    // @Transactional) means one always finishes and releases its lock
    // before the next begins, so the cycle can't form.
    public AuthResponse login(LoginRequest request) {
        List<User> candidates = userRepository.findAllByEmailIgnoreCaseAndActiveTrue(request.email());

        User matched = candidates.stream()
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        Tenant tenant = tenantRepository
                .findById(matched.getTenantId())
                .orElseThrow(() -> new IllegalStateException("User references a missing tenant"));
        if (!tenant.isActive()) {
            throw new AccountSuspendedException("This business account has been suspended. Contact support.");
        }
        if (tenant.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED) {
            throw new AccountSuspendedException(
                    "Your subscription payment is overdue. Please pay your outstanding invoice to continue.");
        }

        String token = authTokenService.issue(
                matched.getId(), matched.getTenantId(), matched.getEmail(), matched.getRole());

        matched.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(matched);

        return new AuthResponse(
                token, tenant.getId(), matched.getId(), matched.getEmail(), matched.getRole(), tenant.getBusinessName());
    }

    public void logout(UUID tokenId) {
        authTokenService.revoke(tokenId);
    }
}
