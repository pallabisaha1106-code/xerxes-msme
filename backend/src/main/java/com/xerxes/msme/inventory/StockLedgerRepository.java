package com.xerxes.msme.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockLedgerRepository extends JpaRepository<StockLedger, UUID> {

    List<StockLedger> findAllByTenantIdAndVariantIdOrderByCreatedAtDesc(UUID tenantId, UUID variantId);

    boolean existsByTenantIdAndVariantId(UUID tenantId, UUID variantId);
}
