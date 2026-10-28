package com.xerxes.msme.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, UUID> {

    List<ProductVariantImage> findAllByVariantIdOrderByDisplayOrderAsc(UUID variantId);

    Optional<ProductVariantImage> findByIdAndTenantId(UUID id, UUID tenantId);

    int countByVariantId(UUID variantId);
}
