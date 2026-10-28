package com.xerxes.msme.marketplace;

import com.xerxes.msme.tenant.BusinessType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, UUID> {

    Optional<MarketplaceListing> findByTenantId(UUID tenantId);

    List<MarketplaceListing> findAllByStatusOrderByCreatedAtDesc(MarketplaceListingStatus status);

    List<MarketplaceListing> findAllByOrderByCreatedAtDesc();

    /** A local partner's review queue: only their own shops' listings. */
    List<MarketplaceListing> findAllByTenantIdInOrderByCreatedAtDesc(List<UUID> tenantIds);

    List<MarketplaceListing> findAllByTenantIdInAndStatusOrderByCreatedAtDesc(List<UUID> tenantIds, MarketplaceListingStatus status);

    /** The public feed's "does this tenant even have a live shop page" check - see PublicMarketplaceService. */
    List<MarketplaceListing> findAllByTenantIdInAndStatus(List<UUID> tenantIds, MarketplaceListingStatus status);

    /**
     * Paged public listings browse - categoryId, tenantIds and businessType are all optional (pass null to
     * skip that filter), so this one query covers the "browse everything" and "scoped to a
     * chosen city/area's tenants" cases alike. Slice, not Page - no COUNT query, which matters
     * once this table has thousands of rows and the UI only ever needs "is there a next page".
     */
    @Query("""
            SELECT m FROM MarketplaceListing m
            WHERE m.status = :status
              AND (:categoryId IS NULL OR m.categoryId = :categoryId)
              AND (:tenantIds IS NULL OR m.tenantId IN :tenantIds)
              AND (:businessType IS NULL OR m.tenantId IN (
                    SELECT t.id FROM Tenant t
                    WHERE t.businessType = :businessType
                       OR (:untypedIsRetail = true AND t.businessType IS NULL)))
            ORDER BY m.createdAt DESC
            """)
    Slice<MarketplaceListing> findLiveListingsPage(
            @Param("status") MarketplaceListingStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("tenantIds") List<UUID> tenantIds,
            @Param("businessType") BusinessType businessType,
            @Param("untypedIsRetail") boolean untypedIsRetail,
            Pageable pageable);
}
