package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private StorefrontOrderRepository storefrontOrderRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerScope partnerScope;

    private AdminTenantService adminTenantService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        adminTenantService = new AdminTenantService(
                tenantRepository,
                productRepository,
                customerRepository,
                invoiceRepository,
                storefrontOrderRepository,
                subscriptionService,
                partnerRepository,
                partnerScope);
        tenantId = UUID.randomUUID();

        lenient().when(productRepository.countByTenantId(any())).thenReturn(3L);
        lenient().when(customerRepository.countByTenantId(any())).thenReturn(5L);
        lenient().when(invoiceRepository.countByTenantId(any())).thenReturn(7L);
        lenient().when(storefrontOrderRepository.countByTenantId(any())).thenReturn(2L);
    }

    private Tenant realVendor() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        tenant.setEmail("veera@test.com");
        tenant.setStateCode("27");
        tenant.setPlatform(false);
        return tenant;
    }

    @Test
    void listTenants_returnsOnlyNonPlatformTenantsWithUsageCounts() {
        when(tenantRepository.findAllByPlatformFalseOrderByCreatedAtDesc()).thenReturn(List.of(realVendor()));

        List<AdminTenantResponse> result = adminTenantService.listTenants();

        assertThat(result).hasSize(1);
        AdminTenantResponse response = result.get(0);
        assertThat(response.businessName()).isEqualTo("Veera Collections");
        assertThat(response.productCount()).isEqualTo(3);
        assertThat(response.customerCount()).isEqualTo(5);
        assertThat(response.invoiceCount()).isEqualTo(7);
        assertThat(response.storefrontOrderCount()).isEqualTo(2);
    }

    @Test
    void getTenant_realVendor_returnsProfile() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(realVendor()));

        AdminTenantResponse response = adminTenantService.getTenant(tenantId);

        assertThat(response.id()).isEqualTo(tenantId);
    }

    @Test
    void getTenant_showsThePartnerLookingAfterTheShop_orNone() {
        Tenant tenant = realVendor();
        UUID partnerId = UUID.randomUUID();
        tenant.setPartnerId(partnerId);
        Partner partner = new Partner();
        partner.setName("Meera");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));

        assertThat(adminTenantService.getTenant(tenantId).partnerName()).isEqualTo("Meera");

        tenant.setPartnerId(null);
        assertThat(adminTenantService.getTenant(tenantId).partnerName()).isNull();
    }

    @Test
    void listTenants_asAPartner_returnsOnlyTheirShopsWithoutSalesCounts() {
        UUID partnerId = UUID.randomUUID();
        when(partnerScope.partnerId()).thenReturn(Optional.of(partnerId));
        when(partnerScope.isPartner()).thenReturn(true);
        when(tenantRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId)).thenReturn(List.of(realVendor()));

        List<AdminTenantResponse> result = adminTenantService.listTenants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productCount()).isEqualTo(3);
        assertThat(result.get(0).customerCount()).isZero();
        assertThat(result.get(0).invoiceCount()).isZero();
        assertThat(result.get(0).storefrontOrderCount()).isZero();
        verify(customerRepository, never()).countByTenantId(any());
        verify(tenantRepository, never()).findAllByPlatformFalseOrderByCreatedAtDesc();
    }

    @Test
    void getTenant_aShopThePartnerDoesNotLookAfter_isNotFound() {
        Tenant tenant = realVendor();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        org.mockito.Mockito.doThrow(ResourceNotFoundException.of("Tenant", tenantId)).when(partnerScope).requireAccess(tenant);

        assertThrows(ResourceNotFoundException.class, () -> adminTenantService.getTenant(tenantId));
    }

    @Test
    void updateStatus_logsTheDecision() {
        Tenant tenant = realVendor();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        adminTenantService.updateStatus(tenantId, true);
        adminTenantService.updateStatus(tenantId, false);

        verify(partnerScope).record(tenantId, ReviewActionType.ACCOUNT_ACTIVATED, null, null);
        verify(partnerScope).record(tenantId, ReviewActionType.ACCOUNT_SUSPENDED, null, null);
    }

    private Partner partnerWithStatus(PartnerStatus status) {
        Partner partner = new Partner();
        partner.setId(UUID.randomUUID());
        partner.setName("Meera");
        partner.setStatus(status);
        return partner;
    }

    @Test
    void assignPartner_givesTheShopToAnActivePartnerAndLogsIt() {
        Tenant tenant = realVendor();
        Partner partner = partnerWithStatus(PartnerStatus.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(partnerRepository.findById(partner.getId())).thenReturn(Optional.of(partner));

        AdminTenantResponse response = adminTenantService.assignPartner(tenantId, partner.getId());

        assertThat(tenant.getPartnerId()).isEqualTo(partner.getId());
        assertThat(tenant.getPartnerAssignedAt()).isNotNull();
        assertThat(response.partnerName()).isEqualTo("Meera");
        assertThat(response.partnerActive()).isTrue();
        verify(partnerScope).record(tenantId, ReviewActionType.PARTNER_ASSIGNED, partner.getId(), null);
    }

    @Test
    void assignPartner_movingAShopThatHadAPartnerIsLoggedAsAReassignment() {
        Tenant tenant = realVendor();
        tenant.setPartnerId(UUID.randomUUID());
        Partner partner = partnerWithStatus(PartnerStatus.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(partnerRepository.findById(partner.getId())).thenReturn(Optional.of(partner));

        adminTenantService.assignPartner(tenantId, partner.getId());

        verify(partnerScope).record(tenantId, ReviewActionType.PARTNER_REASSIGNED, partner.getId(), null);
    }

    @Test
    void assignPartner_aPartnerWhoIsNotActive_isRefused() {
        Tenant tenant = realVendor();
        Partner off = partnerWithStatus(PartnerStatus.INACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(partnerRepository.findById(off.getId())).thenReturn(Optional.of(off));

        assertThrows(BusinessRuleViolationException.class, () -> adminTenantService.assignPartner(tenantId, off.getId()));

        assertThat(tenant.getPartnerId()).isNull();
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void assignPartner_withNoPartner_takesTheShopBackToSuperAdmin() {
        Tenant tenant = realVendor();
        tenant.setPartnerId(UUID.randomUUID());
        tenant.setPartnerAssignedAt(java.time.OffsetDateTime.now());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        AdminTenantResponse response = adminTenantService.assignPartner(tenantId, null);

        assertThat(tenant.getPartnerId()).isNull();
        assertThat(tenant.getPartnerAssignedAt()).isNull();
        assertThat(response.partnerActive()).isNull();
        verify(partnerScope).record(tenantId, ReviewActionType.PARTNER_REASSIGNED, null, null);
    }

    @Test
    void getTenant_aShopWhosePartnerWasSwitchedOff_saysSo() {
        Tenant tenant = realVendor();
        Partner off = partnerWithStatus(PartnerStatus.INACTIVE);
        tenant.setPartnerId(off.getId());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(partnerRepository.findById(off.getId())).thenReturn(Optional.of(off));

        assertThat(adminTenantService.getTenant(tenantId).partnerActive()).isFalse();
    }

    @Test
    void getTenant_platformSentinelTenant_isHiddenAsNotFound() {
        Tenant platformTenant = realVendor();
        platformTenant.setPlatform(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(platformTenant));

        assertThrows(ResourceNotFoundException.class, () -> adminTenantService.getTenant(tenantId));
    }

    @Test
    void getTenant_unknownId_throwsResourceNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminTenantService.getTenant(tenantId));
    }

    @Test
    void updateStatus_deactivatesTenant() {
        Tenant tenant = realVendor();
        tenant.setActive(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        AdminTenantResponse response = adminTenantService.updateStatus(tenantId, false);

        assertThat(response.active()).isFalse();
        verify(tenantRepository).save(tenant);
    }

    @Test
    void updateStatus_platformSentinelTenant_cannotBeModified() {
        Tenant platformTenant = realVendor();
        platformTenant.setPlatform(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(platformTenant));

        assertThrows(ResourceNotFoundException.class, () -> adminTenantService.updateStatus(tenantId, false));
    }

    @Test
    void updatePlan_schedulesTheChangeForNextCycleWithoutChangingTheCurrentPlan() {
        Tenant tenant = realVendor();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        AdminTenantResponse response = adminTenantService.updatePlan(tenantId, MembershipType.PLUS);

        assertThat(response.subscriptionPlan()).isEqualTo(MembershipType.FREE);
        assertThat(response.pendingSubscriptionPlan()).isEqualTo(MembershipType.PLUS);
        assertThat(tenant.getPendingSubscriptionPlan()).isEqualTo(MembershipType.PLUS);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void updatePlan_tenantStillInTrial_throwsBusinessRuleViolation() {
        Tenant tenant = realVendor();
        tenant.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThrows(BusinessRuleViolationException.class, () -> adminTenantService.updatePlan(tenantId, MembershipType.PLUS));

        assertThat(tenant.getPendingSubscriptionPlan()).isNull();
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void updateStatus_approving_leavesTheRegistrationCodeAlone() {
        Tenant tenant = realVendor();
        tenant.setActive(false);
        tenant.setRegistrationCode("ABCD1234");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        adminTenantService.updateStatus(tenantId, true);

        assertThat(tenant.getRegistrationCode()).isEqualTo("ABCD1234");
    }

    @Test
    void updateStatus_deactivating_leavesAnyExistingRegistrationCodeAlone() {
        Tenant tenant = realVendor();
        tenant.setRegistrationCode("ABCD1234");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        adminTenantService.updateStatus(tenantId, false);

        assertThat(tenant.getRegistrationCode()).isEqualTo("ABCD1234");
    }

    @Test
    void updateSubscription_delegatesToSubscriptionServiceOverride() {
        Tenant tenant = realVendor();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        adminTenantService.updateSubscription(tenantId, SubscriptionStatus.ACTIVE, null);

        verify(subscriptionService).adminOverride(tenantId, SubscriptionStatus.ACTIVE, null);
    }

    @Test
    void updateSubscription_platformSentinelTenant_cannotBeModified() {
        Tenant platformTenant = realVendor();
        platformTenant.setPlatform(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(platformTenant));

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminTenantService.updateSubscription(tenantId, SubscriptionStatus.ACTIVE, null));
    }

    @Test
    void generateSubscriptionInvoice_delegatesToSubscriptionService() {
        Tenant tenant = realVendor();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        com.xerxes.msme.subscription.dto.PendingInvoiceResponse response =
                new com.xerxes.msme.subscription.dto.PendingInvoiceResponse(99900, "https://rzp.io/i/x", java.time.OffsetDateTime.now());
        when(subscriptionService.generateInvoiceForTenant(tenantId)).thenReturn(response);

        var result = adminTenantService.generateSubscriptionInvoice(tenantId);

        assertThat(result).isSameAs(response);
    }

    @Test
    void generateSubscriptionInvoice_platformSentinelTenant_cannotBeModified() {
        Tenant platformTenant = realVendor();
        platformTenant.setPlatform(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(platformTenant));

        assertThrows(ResourceNotFoundException.class, () -> adminTenantService.generateSubscriptionInvoice(tenantId));
    }
}
