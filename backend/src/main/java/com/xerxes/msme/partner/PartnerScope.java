package com.xerxes.msme.partner;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Narrows the admin services to what the caller may see: Super Admin sees every shop, a local partner only the shops
 * assigned to them. A shop that is not theirs is reported as "not found", the same as one that does not exist, so a
 * partner cannot tell the two apart. Also writes the review log with the caller as the actor.
 */
@Component
public class PartnerScope {

    private final AuthContext authContext;
    private final PartnerContext partnerContext;
    private final TenantRepository tenantRepository;
    private final ReviewActionService reviewActionService;

    public PartnerScope(
            AuthContext authContext,
            PartnerContext partnerContext,
            TenantRepository tenantRepository,
            ReviewActionService reviewActionService) {
        this.authContext = authContext;
        this.partnerContext = partnerContext;
        this.tenantRepository = tenantRepository;
        this.reviewActionService = reviewActionService;
    }

    public boolean isPartner() {
        return authContext.current().role() == Role.PARTNER;
    }

    /** The calling partner's id (which must still be ACTIVE), or empty for Super Admin, who is not narrowed. */
    public Optional<UUID> partnerId() {
        return isPartner() ? Optional.of(partnerContext.current().getId()) : Optional.empty();
    }

    /** The ids of the shops the caller is limited to, or empty for Super Admin. */
    public Optional<List<UUID>> ownedTenantIds() {
        return partnerId().map(id -> tenantRepository.findAllByPartnerIdOrderByCreatedAtDesc(id).stream().map(Tenant::getId).toList());
    }

    public void requireAccess(Tenant tenant) {
        partnerId().ifPresent(id -> {
            if (!id.equals(tenant.getPartnerId())) {
                throw ResourceNotFoundException.of("Tenant", tenant.getId());
            }
        });
    }

    public void requireAccess(UUID tenantId) {
        partnerId().ifPresent(id -> {
            boolean owned = tenantRepository.findById(tenantId).map(tenant -> id.equals(tenant.getPartnerId())).orElse(false);
            if (!owned) {
                throw ResourceNotFoundException.of("Tenant", tenantId);
            }
        });
    }

    /** Logs a decision on a shop, by the signed-in caller (and their partner record, when they are a partner). */
    public void record(UUID tenantId, ReviewActionType action, UUID subjectId, String reason) {
        reviewActionService.record(
                tenantId, authContext.current().userId(), partnerId().orElse(null), action, subjectId, reason);
    }
}
