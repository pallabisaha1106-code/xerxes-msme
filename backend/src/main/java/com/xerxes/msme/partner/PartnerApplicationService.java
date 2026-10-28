package com.xerxes.msme.partner;

import com.xerxes.msme.appointment.PhoneNumbers;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ServiceArea;
import com.xerxes.msme.marketplace.ServiceAreaRepository;
import com.xerxes.msme.partner.dto.PartnerApplicationRequest;
import com.xerxes.msme.partner.dto.PartnerApplicationResponse;
import com.xerxes.msme.tenant.PlatformTenant;
import com.xerxes.msme.user.Role;
import com.xerxes.msme.user.User;
import com.xerxes.msme.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Someone applies to become a local partner (public form on the marketplace site). Creates their login - switched off
 * until Super Admin approves, so they cannot sign in yet - the partner row (APPLIED) and the areas they asked for.
 */
@Service
public class PartnerApplicationService {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerAreaRepository partnerAreaRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final PasswordEncoder passwordEncoder;

    public PartnerApplicationService(
            UserRepository userRepository,
            PartnerRepository partnerRepository,
            PartnerAreaRepository partnerAreaRepository,
            ServiceAreaRepository serviceAreaRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.partnerAreaRepository = partnerAreaRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PartnerApplicationResponse apply(PartnerApplicationRequest request) {
        String mobile = PhoneNumbers.normalizeIndianMobile(request.whatsapp())
                .orElseThrow(() -> new BusinessRuleViolationException("Enter a valid Indian mobile number for WhatsApp"));

        // Login looks accounts up by email alone, and the users table is only unique per tenant, so a partner's email
        // has to be unused everywhere or the sign-in could be ambiguous.
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account is already registered with email " + request.email());
        }

        List<ServiceArea> areas = new LinkedHashSet<>(request.areaIds()).stream()
                .map(id -> serviceAreaRepository
                        .findById(id)
                        .filter(ServiceArea::isActive)
                        .orElseThrow(() -> ResourceNotFoundException.of("ServiceArea", id)))
                .toList();

        User user = new User();
        user.setTenantId(PlatformTenant.ID);
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.PARTNER);
        user.setActive(false); // switched on when Super Admin approves
        user = userRepository.save(user);

        Partner partner = new Partner();
        partner.setUserId(user.getId());
        partner.setName(request.name().trim());
        partner.setWhatsapp("91" + mobile);
        partner.setStatus(PartnerStatus.APPLIED);
        partner = partnerRepository.save(partner);

        for (ServiceArea area : areas) {
            PartnerArea partnerArea = new PartnerArea();
            partnerArea.setPartnerId(partner.getId());
            partnerArea.setAreaId(area.getId());
            partnerAreaRepository.save(partnerArea);
        }
        return new PartnerApplicationResponse(partner.getId(), partner.getName());
    }
}
