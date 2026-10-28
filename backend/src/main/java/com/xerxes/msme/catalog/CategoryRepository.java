package com.xerxes.msme.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByTenantId(UUID tenantId);

    Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    Optional<Category> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
