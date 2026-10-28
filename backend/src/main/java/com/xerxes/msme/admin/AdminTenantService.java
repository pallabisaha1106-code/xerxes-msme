package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminTenantResponse;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.sales.CustomerRepository;
import com.xerxes.msme.sales.InvoiceRepository;
import com.xerxes.msme.storefront.StorefrontOrderRepository;
import com.xerxes.msme.partner.Partner;
import com.xerxes.msme.partner.PartnerRepository;
import com.xerxes.msme.partner.PartnerScope;
import com.xerxes.msme.partner.PartnerStatus;
import com.xerxes.msme.partner.ReviewActionType;
import com.xerxes.msme.subscription.SubscriptionService;
import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.subscription.dto.PendingInvoiceResponse;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-staff view across every vendor - the sentinel platform tenant
 * itself (see Tenant.platform) is always excluded, both from the directory
 * and from single-tenant lookups, so it never shows up as a "vendor" and
 * can't be deactivated or reassigned a plan through this API.
 */
@Service
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final StorefrontOrderRepository storefrontOrderRepository;
    private final SubscriptionService subscriptionService;
    private final PartnerRepository partnerRepository;
    private final PartnerScope partnerScope;

    public AdminTenantService(
            TenantRepository tenantRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            StorefrontOrderRepository storefrontOrderRepository,
            SubscriptionService subscriptionService,
            PartnerRepository partnerRepository,
            PartnerScope partnerScope) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.storefrontOrderRepository = storefrontOrderRepository;
        this.subscriptionService = subscriptionService;
        this.partnerRepository = partnerRepository;
        this.partnerScope = partnerScope;
    }

    @Transactional(readOnly = true)
    public List<AdminTenantResponse> listTenants() {
        return partnerScope
                .partnerId()
                .map(tenantRepository::findAllByPartnerIdOrderByCreatedAtDesc)
                .orElseGet(tenantRepository::findAllByPlatformFalseOrderByCreatedAtDesc)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTenantResponse getTenant(UUID tenantId) {
        return toResponse(findVendor(tenantId));
    }

    @Transactional
    public AdminTenantResponse updateStatus(UUID tenantId, boolean active) {
        Tenant tenant = findVendor(tenantId);
        tenant.setActive(active);
        // registrationCode is deliberately left alone here (kept even after
        // approval) - see VendorApplicationService for what it still grants.
        tenantRepository.save(tenant);
        partnerScope.record(tenantId, active ? ReviewActionType.ACCOUNT_ACTIVATED : ReviewActionType.ACCOUNT_SUSPENDED, null, null);
        return toResponse(tenant);
    }

    /** Gives the shop to the chosen (active) partner, or with a null partnerId back to Super Admin's own queue. */
    @Transactional
    public AdminTenantResponse assignPartner(UUID tenantId, UUID partnerId) {
        Tenant tenant = findVendor(tenantId);
        ReviewActionType action =
                tenant.getPartnerId() == null ? ReviewActionType.PARTNER_ASSIGNED : ReviewActionType.PARTNER_REASSIGNED;
        if (partnerId != null) {
            Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> ResourceNotFoundException.of("Partner", partnerId));
            if (partner.getStatus() != PartnerStatus.ACTIVE) {
                throw new BusinessRuleViolationException("Only an active partner can be given a shop.");
            }
        }
        tenant.setPartnerId(partnerId);
        tenant.setPartnerAssignedAt(partnerId == null ? null : OffsetDateTime.now());
        tenantRepository.save(tenant);
        partnerScope.record(tenantId, action, partnerId, null);
        return toResponse(tenant);
    }

    @Transactional
    public AdminTenantResponse updatePlan(UUID tenantId, MembershipType subscriptionPlan) {
        Tenant tenant = findVendor(tenantId);
        if (tenant.getSubscriptionStatus() == SubscriptionStatus.TRIALING) {
            throw new BusinessRuleViolationException(
                    "Cannot change plan during the trial period - wait until the trial ends.");
        }
        // Takes effect at the next billing cycle, not immediately - see SubscriptionService.
        tenant.setPendingSubscriptionPlan(subscriptionPlan);
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public AdminTenantResponse updateSubscription(UUID tenantId, SubscriptionStatus status, OffsetDateTime currentPeriodEnd) {
        findVendor(tenantId); // 404s for a platform/unknown tenant before delegating
        subscriptionService.adminOverride(tenantId, status, currentPeriodEnd);
        return toResponse(findVendor(tenantId));
    }

    @Transactional
    public PendingInvoiceResponse generateSubscriptionInvoice(UUID tenantId) {
        findVendor(tenantId); // 404s for a platform/unknown tenant before delegating
        return subscriptionService.generateInvoiceForTenant(tenantId);
    }

    private Tenant findVendor(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        if (tenant.isPlatform()) {
            throw ResourceNotFoundException.of("Tenant", tenantId);
        }
        partnerScope.requireAccess(tenant);
        return tenant;
    }

    private AdminTenantResponse toResponse(Tenant tenant) {
        UUID id = tenant.getId();
        // A partner never sees what the shop sells or earns: those counts are not even looked up for them.
        boolean viewedByPartner = partnerScope.isPartner();
        Partner partner = tenant.getPartnerId() == null ? null : partnerRepository.findById(tenant.getPartnerId()).orElse(null);
        return new AdminTenantResponse(
                id,
                tenant.getBusinessName(),
                tenant.getEmail(),
                tenant.getPhone(),
                tenant.getGstin(),
                tenant.getStateCode(),
                tenant.getAddressLine1(),
                tenant.getAddressLine2(),
                tenant.getCity(),
                tenant.getPincode(),
                tenant.getSubscriptionPlan(),
                tenant.getPendingSubscriptionPlan(),
                tenant.isActive(),
                tenant.getCreatedAt(),
                productRepository.countByTenantId(id),
                viewedByPartner ? 0 : customerRepository.countByTenantId(id),
                viewedByPartner ? 0 : invoiceRepository.countByTenantId(id),
                viewedByPartner ? 0 : storefrontOrderRepository.countByTenantId(id),
                tenant.getSubscriptionStatus(),
                tenant.getTrialEndsAt(),
                tenant.getCurrentPeriodEnd(),
                tenant.getGracePeriodEndsAt(),
                tenant.getRegistrationCode(),
                tenant.getBusinessType(),
                tenant.getShopFrontPhotoUrl(),
                tenant.getGstCertificateUrl(),
                tenant.getOwnerIdDocumentUrl(),
                tenant.getLandmark(),
                partner == null ? null : partner.getName(),
                tenant.getPartnerId(),
                partner == null ? null : partner.getStatus() == PartnerStatus.ACTIVE);
    }
}
