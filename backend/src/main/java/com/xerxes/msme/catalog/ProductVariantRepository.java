package com.xerxes.msme.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findAllByProductId(UUID productId);

    Optional<ProductVariant> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ProductVariant> findByTenantIdAndSkuIgnoreCase(UUID tenantId, String sku);

    boolean existsByTenantIdAndSkuIgnoreCase(UUID tenantId, String sku);

    boolean existsByTenantIdAndBarcode(UUID tenantId, String barcode);

    Optional<ProductVariant> findByTenantIdAndBarcodeAndActiveTrue(UUID tenantId, String barcode);

    long countByProductId(UUID productId);

    // Deactivated variants can't be restocked or sold through the normal
    // flows (they're filtered out of purchase/invoice pickers), so an
    // inactive variant sitting below its reorder threshold isn't an
    // actionable alert - excluded here rather than just in the UI.
    @Query("select v from ProductVariant v where v.tenantId = :tenantId and v.currentStock <= v.reorderThreshold and v.active = true")
    List<ProductVariant> findLowStock(@Param("tenantId") UUID tenantId);

    @Query("select sum(v.currentStock * v.purchasePrice) from ProductVariant v where v.tenantId = :tenantId and v.active = true")
    BigDecimal sumInventoryValue(@Param("tenantId") UUID tenantId);
}
