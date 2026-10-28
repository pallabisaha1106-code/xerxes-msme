package com.xerxes.msme.storefront;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontOrderRepository extends JpaRepository<StorefrontOrder, UUID> {

    List<StorefrontOrder> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<StorefrontOrder> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, StorefrontOrderStatus status);

    Optional<StorefrontOrder> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);
}
