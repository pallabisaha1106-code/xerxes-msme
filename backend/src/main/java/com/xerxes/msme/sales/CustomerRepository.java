package com.xerxes.msme.sales;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findAllByTenantId(UUID tenantId);

    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);

    /** Used to match a returning guest storefront order to an existing Customer, rather than creating a duplicate. */
    Optional<Customer> findFirstByTenantIdAndPhone(UUID tenantId, String phone);
}
