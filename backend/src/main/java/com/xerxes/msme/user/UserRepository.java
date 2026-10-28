package com.xerxes.msme.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    /** True if any account at all, in any tenant, already uses this email - partners must be unique platform-wide. */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Email is unique per tenant, not globally, so a login-by-email lookup can
     * return more than one candidate (rare: the same email used at two different
     * businesses). The service layer disambiguates by checking the password
     * against each candidate's hash rather than guessing which tenant to use.
     */
    List<User> findAllByEmailIgnoreCaseAndActiveTrue(String email);
}
