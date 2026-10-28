package com.xerxes.msme.partner;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.marketplace.ServiceCity;
import com.xerxes.msme.marketplace.ServiceCityRepository;
import com.xerxes.msme.partner.dto.AdminPartnerAreaResponse;
import com.xerxes.msme.partner.dto.AdminPartnerResponse;
import com.xerxes.msme.security.AuthTokenService;
import com.xerxes.msme.tenant.TenantRepository;
import com.xerxes.msme.user.User;
import com.xerxes.msme.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin's side of local partners: review an application (approve or reject), switch a partner off and on, and
 * choose the areas they cover. A partner's login is a User; it is switched on only while the partner is ACTIVE, and
 * switching a partner off also ends any sign-in they still have.
 */
@Service
public class AdminPartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerAreaRepository partnerAreaRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final ServiceCityRepository serviceCityRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuthTokenService authTokenService;

    public AdminPartnerService(
            PartnerRepository partnerRepository,
            PartnerAreaRepository partnerAreaRepository,
            ServiceAreaRepository serviceAreaRepository,
            ServiceCityRepository serviceCityRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            AuthTokenService authTokenService) {
        this.partnerRepository = partnerRepository;
        this.partnerAreaRepository = partnerAreaRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.serviceCityRepository = serviceCityRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.authTokenService = authTokenService;
    }

    /** Every partner, newest first; or only those with the given status. */
    @Transactional(readOnly = true)
    public List<AdminPartnerResponse> listPartners(PartnerStatus status) {
        List<Partner> partners = status == null
                ? partnerRepository.findAllByOrderByCreatedAtDesc()
                : partnerRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return partners.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminPartnerResponse getPartner(UUID partnerId) {
        return toResponse(find(partnerId));
    }

    /** Only an application that is waiting can be approved; it switches the partner's login on. */
    @Transactional
    public AdminPartnerResponse approve(UUID partnerId, UUID decidedByUserId) {
        Partner partner = find(partnerId);
        requireStatus(partner, "Only an application waiting for review can be approved", PartnerStatus.APPLIED);
        decide(partner, PartnerStatus.ACTIVE, decidedByUserId, null);
        setLoginActive(partner, true);
        return toResponse(partner);
    }

    @Transactional
    public AdminPartnerResponse reject(UUID partnerId, String reason, UUID decidedByUserId) {
        Partner partner = find(partnerId);
        requireStatus(partner, "Only an application waiting for review can be rejected", PartnerStatus.APPLIED);
        decide(partner, PartnerStatus.REJECTED, decidedByUserId, reason.trim());
        return toResponse(partner);
    }

    /** Switches an active partner off: no new shops, and they are signed out. Their current shops stay assigned. */
    @Transactional
    public AdminPartnerResponse deactivate(UUID partnerId, UUID decidedByUserId) {
        Partner partner = find(partnerId);
        requireStatus(partner, "Only an active partner can be deactivated", PartnerStatus.ACTIVE);
        decide(partner, PartnerStatus.INACTIVE, decidedByUserId, null);
        setLoginActive(partner, false);
        authTokenService.revokeAllForUser(partner.getUserId());
        return toResponse(partner);
    }

    @Transactional
    public AdminPartnerResponse reactivate(UUID partnerId, UUID decidedByUserId) {
        Partner partner = find(partnerId);
        requireStatus(partner, "Only a deactivated partner can be reactivated", PartnerStatus.INACTIVE);
        decide(partner, PartnerStatus.ACTIVE, decidedByUserId, null);
        setLoginActive(partner, true);
        return toResponse(partner);
    }

    /** Makes the partner's areas exactly the given set: adds the new ones and removes the ones left out. */
    @Transactional
    public AdminPartnerResponse replaceAreas(UUID partnerId, List<UUID> areaIds) {
        Partner partner = find(partnerId);
        Set<UUID> wanted = new LinkedHashSet<>(areaIds);
        for (UUID areaId : wanted) {
            serviceAreaRepository
                    .findById(areaId)
                    .filter(ServiceArea::isActive)
                    .orElseThrow(() -> ResourceNotFoundException.of("ServiceArea", areaId));
        }

        List<PartnerArea> current = partnerAreaRepository.findAllByPartnerId(partnerId);
        Set<UUID> currentIds = current.stream().map(PartnerArea::getAreaId).collect(Collectors.toSet());

        // Only the difference is written: deleting and re-adding the same area in one go would hit the unique pair
        // (Hibernate runs inserts before deletes).
        partnerAreaRepository.deleteAll(current.stream().filter(pa -> !wanted.contains(pa.getAreaId())).toList());
        for (UUID areaId : wanted) {
            if (currentIds.contains(areaId)) continue;
            PartnerArea added = new PartnerArea();
            added.setPartnerId(partnerId);
            added.setAreaId(areaId);
            partnerAreaRepository.save(added);
        }
        return toResponse(partner);
    }

    private Partner find(UUID partnerId) {
        return partnerRepository.findById(partnerId).orElseThrow(() -> ResourceNotFoundException.of("Partner", partnerId));
    }

    private void requireStatus(Partner partner, String message, PartnerStatus... allowed) {
        for (PartnerStatus status : allowed) {
            if (partner.getStatus() == status) return;
        }
        throw new BusinessRuleViolationException(message);
    }

    private void decide(Partner partner, PartnerStatus status, UUID decidedByUserId, String reason) {
        partner.setStatus(status);
        partner.setDecidedBy(decidedByUserId);
        partner.setDecidedAt(OffsetDateTime.now());
        partner.setDecisionReason(reason);
        partnerRepository.save(partner);
    }

    private void setLoginActive(Partner partner, boolean active) {
        User user = userRepository
                .findById(partner.getUserId())
                .orElseThrow(() -> new IllegalStateException("Partner references a missing user"));
        user.setActive(active);
        userRepository.save(user);
    }

    private AdminPartnerResponse toResponse(Partner partner) {
        String email = userRepository.findById(partner.getUserId()).map(User::getEmail).orElse(null);

        List<PartnerArea> partnerAreas = partnerAreaRepository.findAllByPartnerId(partner.getId());
        Map<UUID, ServiceArea> areasById = serviceAreaRepository
                .findAllById(partnerAreas.stream().map(PartnerArea::getAreaId).toList())
                .stream()
                .collect(Collectors.toMap(ServiceArea::getId, Function.identity()));
        Map<UUID, ServiceCity> citiesById = serviceCityRepository
                .findAllById(areasById.values().stream().map(ServiceArea::getCityId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ServiceCity::getId, Function.identity()));
        List<AdminPartnerAreaResponse> areas = partnerAreas.stream()
                .map(pa -> areasById.get(pa.getAreaId()))
                .filter(java.util.Objects::nonNull)
                .map(area -> new AdminPartnerAreaResponse(
                        area.getId(), area.getName(), citiesById.containsKey(area.getCityId()) ? citiesById.get(area.getCityId()).getName() : null))
                .toList();

        return new AdminPartnerResponse(
                partner.getId(),
                partner.getName(),
                email,
                partner.getWhatsapp(),
                partner.getStatus(),
                areas,
                tenantRepository.countByPartnerId(partner.getId()),
                partner.getCreatedAt(),
                partner.getDecidedAt(),
                partner.getDecisionReason());
    }
}
