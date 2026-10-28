package com.xerxes.msme.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.subscription.dto.SubscriptionStatusResponse;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private SubscriptionInvoiceRepository invoiceRepository;
    @Mock private RazorpayClient razorpayClient;

    private final SubscriptionProperties properties = new SubscriptionProperties(30, 7);
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(tenantRepository, invoiceRepository, razorpayClient, properties);
    }

    private Tenant tenant(UUID id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setBusinessName("Veera Collections");
        tenant.setEmail("veera@test.com");
        tenant.setPhone("9876543210");
        tenant.setPlatform(false);
        tenant.setActive(true);
        tenant.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        tenant.setCurrentPeriodEnd(OffsetDateTime.now().minusDays(1));
        // Explicit, not relying on Tenant's own default - FREE now has its own no-Razorpay path (see below),
        // and most tests here exercise the normal paid-invoice flow.
        tenant.setSubscriptionPlan(MembershipType.PLUS);
        return tenant;
    }

    @Test
    void generateDueInvoices_dueTenant_createsInvoiceAndPaymentLinkAndMovesToPastDue() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(any()))
                .thenReturn(List.of(tenant));
        when(invoiceRepository.existsByTenantIdAndStatus(tenantId, SubscriptionInvoiceStatus.PENDING)).thenReturn(false);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(inv -> {
            SubscriptionInvoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(razorpayClient.createPaymentLink(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(new RazorpayClient.PaymentLink("plink_123", "https://rzp.io/i/abc"));

        subscriptionService.generateDueInvoices();

        ArgumentCaptor<SubscriptionInvoice> invoiceCaptor = ArgumentCaptor.forClass(SubscriptionInvoice.class);
        verify(invoiceRepository, times(2)).save(invoiceCaptor.capture());
        SubscriptionInvoice savedInvoice = invoiceCaptor.getValue();
        assertThat(savedInvoice.getRazorpayPaymentLinkId()).isEqualTo("plink_123");
        assertThat(savedInvoice.getRazorpayPaymentLinkUrl()).isEqualTo("https://rzp.io/i/abc");
        assertThat(tenant.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(tenant.getGracePeriodEndsAt()).isNotNull();
        verify(tenantRepository).save(tenant);
    }

    @Test
    void generateDueInvoices_tenantHasAPendingPlanChange_appliesItAndPricesTheNewInvoiceAccordingly() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        tenant.setSubscriptionPlan(MembershipType.FREE);
        tenant.setPendingSubscriptionPlan(MembershipType.PLUS);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(any()))
                .thenReturn(List.of(tenant));
        when(invoiceRepository.existsByTenantIdAndStatus(tenantId, SubscriptionInvoiceStatus.PENDING)).thenReturn(false);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(inv -> {
            SubscriptionInvoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(razorpayClient.createPaymentLink(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(new RazorpayClient.PaymentLink("plink_123", "https://rzp.io/i/abc"));

        subscriptionService.generateDueInvoices();

        assertThat(tenant.getSubscriptionPlan()).isEqualTo(MembershipType.PLUS);
        assertThat(tenant.getPendingSubscriptionPlan()).isNull();
        ArgumentCaptor<SubscriptionInvoice> invoiceCaptor = ArgumentCaptor.forClass(SubscriptionInvoice.class);
        verify(invoiceRepository, times(2)).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getAmountPaise()).isEqualTo(MembershipType.PLUS.monthlyAmountPaise());
    }

    @Test
    void generateDueInvoices_freePlanTenant_isAutoPaidAtZeroAndNeverGoesPastDue() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        tenant.setSubscriptionPlan(MembershipType.FREE);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(any()))
                .thenReturn(List.of(tenant));
        when(invoiceRepository.existsByTenantIdAndStatus(tenantId, SubscriptionInvoiceStatus.PENDING)).thenReturn(false);
        OffsetDateTime originalPeriodEnd = tenant.getCurrentPeriodEnd();
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(inv -> {
            SubscriptionInvoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });

        subscriptionService.generateDueInvoices();

        ArgumentCaptor<SubscriptionInvoice> invoiceCaptor = ArgumentCaptor.forClass(SubscriptionInvoice.class);
        verify(invoiceRepository, times(1)).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getAmountPaise()).isEqualTo(0);
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(SubscriptionInvoiceStatus.PAID);
        assertThat(invoiceCaptor.getValue().getPaidAt()).isNotNull();
        assertThat(tenant.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(tenant.getGracePeriodEndsAt()).isNull();
        assertThat(tenant.getCurrentPeriodEnd()).isEqualTo(originalPeriodEnd.plusMonths(1));
        verify(razorpayClient, never()).createPaymentLink(anyLong(), anyString(), anyString(), any(), any());
    }

    @Test
    void generateDueInvoices_tenantAlreadyHasPendingInvoice_skipsIt() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(any()))
                .thenReturn(List.of(tenant));
        when(invoiceRepository.existsByTenantIdAndStatus(tenantId, SubscriptionInvoiceStatus.PENDING)).thenReturn(true);

        subscriptionService.generateDueInvoices();

        verify(razorpayClient, never()).createPaymentLink(anyLong(), anyString(), anyString(), any(), any());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void generateDueInvoices_razorpayFailsForOneTenant_doesNotStopTheBatch() {
        Tenant failing = tenant(UUID.randomUUID());
        Tenant succeeding = tenant(UUID.randomUUID());
        when(tenantRepository.findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(any()))
                .thenReturn(List.of(failing, succeeding));
        when(invoiceRepository.existsByTenantIdAndStatus(any(), any())).thenReturn(false);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(inv -> {
            SubscriptionInvoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(razorpayClient.createPaymentLink(anyLong(), anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("Razorpay down"))
                .thenReturn(new RazorpayClient.PaymentLink("plink_456", "https://rzp.io/i/def"));

        subscriptionService.generateDueInvoices();

        assertThat(succeeding.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        verify(tenantRepository).save(succeeding);
        verify(tenantRepository, never()).save(failing);
    }

    @Test
    void sweepExpiredGracePeriods_suspendsTenantsPastTheirGracePeriod() {
        Tenant tenant = tenant(UUID.randomUUID());
        tenant.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        when(tenantRepository.findAllByPlatformFalseAndSubscriptionStatusAndGracePeriodEndsAtBefore(
                        org.mockito.ArgumentMatchers.eq(SubscriptionStatus.PAST_DUE), any()))
                .thenReturn(List.of(tenant));

        subscriptionService.sweepExpiredGracePeriods();

        assertThat(tenant.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void handleWebhookPaid_pendingInvoice_marksInvoicePaidAndReactivatesTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        OffsetDateTime originalPeriodEnd = tenant.getCurrentPeriodEnd();
        tenant.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        tenant.setGracePeriodEndsAt(OffsetDateTime.now().plusDays(3));

        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(tenantId);
        invoice.setStatus(SubscriptionInvoiceStatus.PENDING);
        invoice.setRazorpayPaymentLinkId("plink_789");

        when(invoiceRepository.findByRazorpayPaymentLinkId("plink_789")).thenReturn(Optional.of(invoice));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        subscriptionService.handleWebhookPaid("plink_789");

        assertThat(invoice.getStatus()).isEqualTo(SubscriptionInvoiceStatus.PAID);
        assertThat(invoice.getPaidAt()).isNotNull();
        assertThat(tenant.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(tenant.getGracePeriodEndsAt()).isNull();
        assertThat(tenant.getCurrentPeriodEnd()).isAfter(originalPeriodEnd);
    }

    @Test
    void handleWebhookPaid_alreadyPaidInvoice_isANoOp() {
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStatus(SubscriptionInvoiceStatus.PAID);
        invoice.setRazorpayPaymentLinkId("plink_already_paid");
        when(invoiceRepository.findByRazorpayPaymentLinkId("plink_already_paid")).thenReturn(Optional.of(invoice));

        subscriptionService.handleWebhookPaid("plink_already_paid");

        verify(invoiceRepository, never()).save(any());
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    void handleWebhookPaid_unknownPaymentLinkId_isANoOp() {
        when(invoiceRepository.findByRazorpayPaymentLinkId("unknown")).thenReturn(Optional.empty());

        subscriptionService.handleWebhookPaid("unknown");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getStatus_includesPendingInvoiceWhenOneExists() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        SubscriptionInvoice pending = new SubscriptionInvoice();
        pending.setAmountPaise(99900);
        pending.setRazorpayPaymentLinkUrl("https://rzp.io/i/pending");
        pending.setCreatedAt(OffsetDateTime.now());
        when(invoiceRepository.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, SubscriptionInvoiceStatus.PENDING))
                .thenReturn(Optional.of(pending));

        SubscriptionStatusResponse response = subscriptionService.getStatus(tenantId);

        assertThat(response.pendingInvoice()).isNotNull();
        assertThat(response.pendingInvoice().amountPaise()).isEqualTo(99900);
        assertThat(response.pendingInvoice().paymentUrl()).isEqualTo("https://rzp.io/i/pending");
    }

    @Test
    void generateInvoiceForTenant_noPendingInvoice_createsOneImmediatelyRegardlessOfCurrentPeriodEnd() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        tenant.setCurrentPeriodEnd(OffsetDateTime.now().plusDays(20)); // period not even due yet
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(invoiceRepository.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, SubscriptionInvoiceStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(inv -> {
            SubscriptionInvoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(razorpayClient.createPaymentLink(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(new RazorpayClient.PaymentLink("plink_manual", "https://rzp.io/i/manual"));

        var response = subscriptionService.generateInvoiceForTenant(tenantId);

        assertThat(response.paymentUrl()).isEqualTo("https://rzp.io/i/manual");
        assertThat(tenant.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void generateInvoiceForTenant_alreadyHasPendingInvoice_returnsItWithoutCreatingAnother() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        SubscriptionInvoice existing = new SubscriptionInvoice();
        existing.setAmountPaise(99900);
        existing.setRazorpayPaymentLinkUrl("https://rzp.io/i/existing");
        existing.setCreatedAt(OffsetDateTime.now());
        when(invoiceRepository.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, SubscriptionInvoiceStatus.PENDING))
                .thenReturn(Optional.of(existing));

        var response = subscriptionService.generateInvoiceForTenant(tenantId);

        assertThat(response.paymentUrl()).isEqualTo("https://rzp.io/i/existing");
        verify(razorpayClient, never()).createPaymentLink(anyLong(), anyString(), anyString(), any(), any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void adminOverride_setsStatusAndClearsGracePeriodWhenLeavingPastDue() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant(tenantId);
        tenant.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        tenant.setGracePeriodEndsAt(OffsetDateTime.now().plusDays(2));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        subscriptionService.adminOverride(tenantId, SubscriptionStatus.ACTIVE, null);

        assertThat(tenant.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(tenant.getGracePeriodEndsAt()).isNull();
        verify(tenantRepository).save(tenant);
    }
}
