package com.xerxes.msme.purchase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseEntryRepository extends JpaRepository<PurchaseEntry, UUID> {

    List<PurchaseEntry> findAllByTenantId(UUID tenantId);

    Optional<PurchaseEntry> findByIdAndTenantId(UUID id, UUID tenantId);
}
