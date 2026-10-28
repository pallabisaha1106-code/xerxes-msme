package com.xerxes.msme.storefront;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontSettingsRepository extends JpaRepository<StorefrontSettings, UUID> {

    Optional<StorefrontSettings> findByTenantId(UUID tenantId);
}
