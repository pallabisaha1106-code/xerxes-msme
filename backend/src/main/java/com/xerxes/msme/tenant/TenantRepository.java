package com.xerxes.msme.tenant;

import com.xerxes.msme.partner.PartnerStatus;
import com.xerxes.msme.subscription.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Tenant> findByEmailIgnoreCase(String email);

    /** Every real vendor, for the platform admin's directory - excludes the sentinel platform tenant itself. */
    List<Tenant> findAllByPlatformFalseOrderByCreatedAtDesc();

    /** Real, active vendors whose current billing period has ended - candidates for the next invoice. */
    List<Tenant> findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(OffsetDateTime cutoff);

    /** PAST_DUE tenants whose grace period has run out - candidates for suspension. */
    List<Tenant> findAllByPlatformFalseAndSubscriptionStatusAndGracePeriodEndsAtBefore(
            SubscriptionStatus subscriptionStatus, OffsetDateTime cutoff);

    boolean existsByRegistrationCode(String registrationCode);

    /** Step 2 of vendor registration - resolving the code an applicant types in. See VendorApplicationService. */
    Optional<Tenant> findByRegistrationCode(String registrationCode);

    /** Public feed candidates filtered by city - see PublicMarketplaceService.listPublicFeed. */
    List<Tenant> findAllByPlatformFalseAndActiveTrueAndCityId(UUID cityId);

    /** The shops a local partner looks after, newest first. */
    List<Tenant> findAllByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    /** Shops in an area that need a partner: they have none yet, or theirs is no longer active. Oldest first. */
    @Query("""
            SELECT t FROM Tenant t
            WHERE t.platform = false AND t.areaId = :areaId
              AND (t.partnerId IS NULL OR t.partnerId NOT IN (SELECT p.id FROM Partner p WHERE p.status = :active))
            ORDER BY t.createdAt
            """)
    List<Tenant> findNeedingPartnerInArea(@Param("areaId") UUID areaId, @Param("active") PartnerStatus active);

    default List<Tenant> findNeedingPartnerInArea(UUID areaId) {
        return findNeedingPartnerInArea(areaId, PartnerStatus.ACTIVE);
    }

    /** Per area, how many shops need a partner (see findNeedingPartnerInArea): rows of [areaId, count]. */
    @Query("""
            SELECT t.areaId, COUNT(t) FROM Tenant t
            WHERE t.platform = false AND t.areaId IS NOT NULL
              AND (t.partnerId IS NULL OR t.partnerId NOT IN (SELECT p.id FROM Partner p WHERE p.status = :active))
            GROUP BY t.areaId
            """)
    List<Object[]> countNeedingPartnerByArea(@Param("active") PartnerStatus active);

    default List<Object[]> countNeedingPartnerByArea() {
        return countNeedingPartnerByArea(PartnerStatus.ACTIVE);
    }

    /** How many shops a local partner currently looks after. */
    long countByPartnerId(UUID partnerId);

    /** Same, narrowed further to a specific area within that city. */
    List<Tenant> findAllByPlatformFalseAndActiveTrueAndCityIdAndAreaId(UUID cityId, UUID areaId);
}
