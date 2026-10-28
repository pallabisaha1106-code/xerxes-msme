package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductCapacityResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only view of the same limit {@link ProductService#createProduct} enforces, so a screen that creates many
 * products at once can say "12 slots left" up front instead of failing partway through.
 */
@Service
public class ProductCapacityService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;

    public ProductCapacityService(TenantRepository tenantRepository, ProductRepository productRepository) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ProductCapacityResponse capacity(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        int limit = tenant.getSubscriptionPlan().productLimit();
        long used = productRepository.countByTenantId(tenantId);
        return new ProductCapacityResponse(limit, used, Math.max(0, limit - used));
    }
}
