package com.xerxes.msme.partner;

import com.xerxes.msme.partner.dto.PartnerContactResponse;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The contact a shop owner sees for their local partner. Only an active partner is shown: if the partner has been
 * switched off the owner is told the platform team will contact them instead, until the shop is moved to someone else.
 */
@Service
public class PartnerContactService {

    private final PartnerRepository partnerRepository;
    private final TenantRepository tenantRepository;

    public PartnerContactService(PartnerRepository partnerRepository, TenantRepository tenantRepository) {
        this.partnerRepository = partnerRepository;
        this.tenantRepository = tenantRepository;
    }

    public static PartnerContactResponse contactOf(Partner partner) {
        return new PartnerContactResponse(partner.getName(), partner.getWhatsapp());
    }

    @Transactional(readOnly = true)
    public Optional<PartnerContactResponse> contactFor(Tenant tenant) {
        if (tenant.getPartnerId() == null) return Optional.empty();
        return partnerRepository
                .findById(tenant.getPartnerId())
                .filter(partner -> partner.getStatus() == PartnerStatus.ACTIVE)
                .map(PartnerContactService::contactOf);
    }

    @Transactional(readOnly = true)
    public Optional<PartnerContactResponse> contactFor(UUID tenantId) {
        return tenantRepository.findById(tenantId).flatMap(this::contactFor);
    }
}
