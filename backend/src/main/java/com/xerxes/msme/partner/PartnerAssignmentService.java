package com.xerxes.msme.partner;

import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands a new shop to a local partner. Every shop belongs to exactly one partner, chosen from the active partners who
 * cover the shop's area, taking turns: the one who was given a shop longest ago goes first. A shop with no area, or
 * in an area nobody active covers, stays unassigned and waits in Super Admin's own queue.
 */
@Service
public class PartnerAssignmentService {

    private final PartnerRepository partnerRepository;
    private final PartnerAreaRepository partnerAreaRepository;
    private final TenantRepository tenantRepository;
    private final ReviewActionService reviewActionService;

    public PartnerAssignmentService(
            PartnerRepository partnerRepository,
            PartnerAreaRepository partnerAreaRepository,
            TenantRepository tenantRepository,
            ReviewActionService reviewActionService) {
        this.partnerRepository = partnerRepository;
        this.partnerAreaRepository = partnerAreaRepository;
        this.tenantRepository = tenantRepository;
        this.reviewActionService = reviewActionService;
    }

    /** The partner who should get the next shop: active, and never given one before or given one longest ago. */
    static Optional<Partner> chooseNext(List<Partner> candidates) {
        return candidates.stream()
                .filter(partner -> partner.getStatus() == PartnerStatus.ACTIVE)
                .min(Comparator.comparing(Partner::getLastAssignedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Partner::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** Assigns a new shop to a partner if one covers its area (automatic: no acting user). */
    @Transactional
    public Optional<Partner> assign(Tenant tenant) {
        return assign(tenant, null);
    }

    /** Gives every shop in the area that needs a partner to one covering it; returns how many were assigned. */
    @Transactional
    public int assignWaiting(UUID areaId, UUID actorUserId) {
        int assigned = 0;
        for (Tenant tenant : tenantRepository.findNeedingPartnerInArea(areaId)) {
            if (assign(tenant, actorUserId).isPresent()) assigned++;
        }
        return assigned;
    }

    /** Assigns the shop to a partner if one covers its area; returns who, or empty when it stays unassigned. */
    private Optional<Partner> assign(Tenant tenant, UUID actorUserId) {
        if (tenant.getAreaId() == null) return Optional.empty();

        List<UUID> partnerIds = partnerAreaRepository.findAllByAreaId(tenant.getAreaId()).stream()
                .map(PartnerArea::getPartnerId)
                .toList();
        if (partnerIds.isEmpty()) return Optional.empty();

        Optional<Partner> chosen = chooseNext(partnerRepository.findAllById(partnerIds));
        chosen.ifPresent(partner -> {
            OffsetDateTime now = OffsetDateTime.now();
            ReviewActionType action =
                    tenant.getPartnerId() == null ? ReviewActionType.PARTNER_ASSIGNED : ReviewActionType.PARTNER_REASSIGNED;
            tenant.setPartnerId(partner.getId());
            tenant.setPartnerAssignedAt(now);
            tenantRepository.save(tenant);
            partner.setLastAssignedAt(now);
            partnerRepository.save(partner);
            reviewActionService.record(tenant.getId(), actorUserId, partner.getId(), action, null, null);
        });
        return chosen;
    }
}
