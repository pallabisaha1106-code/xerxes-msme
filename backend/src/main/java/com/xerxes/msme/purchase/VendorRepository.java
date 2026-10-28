package com.xerxes.msme.purchase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByTenantId(UUID tenantId);

    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);
}
