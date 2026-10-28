package com.xerxes.msme.subscription;

import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.subscription.dto.PendingInvoiceResponse;
import com.xerxes.msme.subscription.dto.SubscriptionStatusResponse;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final TenantRepository tenantRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final RazorpayClient razorpayClient;
    private final SubscriptionProperties subscriptionProperties;

    public SubscriptionService(
            TenantRepository tenantRepository,
            SubscriptionInvoiceRepository invoiceRepository,
            RazorpayClient razorpayClient,
            SubscriptionProperties subscriptionProperties) {
        this.tenantRepository = tenantRepository;
        this.invoiceRepository = invoiceRepository;
        this.razorpayClient = razorpayClient;
        this.subscriptionProperties = subscriptionProperties;
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getStatus(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        PendingInvoiceResponse pendingInvoice = invoiceRepository
                .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, SubscriptionInvoiceStatus.PENDING)
                .map(invoice -> new PendingInvoiceResponse(
                        invoice.getAmountPaise(), invoice.getRazorpayPaymentLinkUrl(), invoice.getCreatedAt()))
                .orElse(null);
        return new SubscriptionStatusResponse(
                tenant.getSubscriptionStatus(),
                tenant.getTrialEndsAt(),
                tenant.getCurrentPeriodEnd(),
                tenant.getGracePeriodEndsAt(),
                pendingInvoice);
    }

    /** Daily job step 1: find tenants whose current period has ended and generate their next invoice + payment link. */
    @Transactional
    public void generateDueInvoices() {
        OffsetDateTime now = OffsetDateTime.now();
        for (Tenant tenant : tenantRepository.findAllByPlatformFalseAndActiveTrueAndCurrentPeriodEndBefore(now)) {
            if (invoiceRepository.existsByTenantIdAndStatus(tenant.getId(), SubscriptionInvoiceStatus.PENDING)) {
                continue;
            }
            try {
                createInvoiceAndPaymentLink(tenant, now);
            } catch (RuntimeException ex) {
                // One tenant's Razorpay call failing (network blip, bad customer
                // data) shouldn't stop the rest of the batch from being billed.
                log.error("Failed to generate subscription invoice for tenant {}", tenant.getId(), ex);
            }
        }
    }

    /**
     * Super Admin's manual "generate now" action (AdminTenantController) -
     * doesn't wait for the tenant's period to actually end, unlike
     * generateDueInvoices(). Returns the existing pending invoice instead of
     * creating a second one if the tenant already has one outstanding.
     */
    @Transactional
    public PendingInvoiceResponse generateInvoiceForTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        SubscriptionInvoice invoice = invoiceRepository
                .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, SubscriptionInvoiceStatus.PENDING)
                .orElseGet(() -> createInvoiceAndPaymentLink(tenant, OffsetDateTime.now()));
        return new PendingInvoiceResponse(invoice.getAmountPaise(), invoice.getRazorpayPaymentLinkUrl(), invoice.getCreatedAt());
    }

    private SubscriptionInvoice createInvoiceAndPaymentLink(Tenant tenant, OffsetDateTime now) {
        // A plan change from AdminTenantService.updatePlan is scheduled here, not applied immediately -
        // it takes effect (and prices) starting with the cycle this new invoice covers.
        if (tenant.getPendingSubscriptionPlan() != null) {
            tenant.setSubscriptionPlan(tenant.getPendingSubscriptionPlan());
            tenant.setPendingSubscriptionPlan(null);
        }

        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setTenantId(tenant.getId());
        invoice.setPeriodStart(tenant.getCurrentPeriodEnd());
        invoice.setPeriodEnd(tenant.getCurrentPeriodEnd().plusMonths(1));
        invoice.setAmountPaise(tenant.getSubscriptionPlan().monthlyAmountPaise());

        if (tenant.getSubscriptionPlan() == MembershipType.FREE) {
            // Nothing to actually collect - mark it paid and roll the period forward immediately,
            // the same outcome handleWebhookPaid gives a real payment, without ever calling Razorpay.
            invoice.setStatus(SubscriptionInvoiceStatus.PAID);
            invoice.setPaidAt(now);
            invoice = invoiceRepository.save(invoice);
            tenant.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            tenant.setCurrentPeriodEnd(tenant.getCurrentPeriodEnd().plusMonths(1));
            tenantRepository.save(tenant);
            return invoice;
        }

        invoice.setStatus(SubscriptionInvoiceStatus.PENDING);
        invoice = invoiceRepository.save(invoice);

        RazorpayClient.PaymentLink paymentLink = razorpayClient.createPaymentLink(
                invoice.getAmountPaise(),
                "Xerxes MSME subscription - " + tenant.getBusinessName(),
                invoice.getId().toString(),
                tenant.getEmail(),
                tenant.getPhone());
        invoice.setRazorpayPaymentLinkId(paymentLink.id());
        invoice.setRazorpayPaymentLinkUrl(paymentLink.shortUrl());
        invoiceRepository.save(invoice);

        tenant.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        tenant.setGracePeriodEndsAt(now.plusDays(subscriptionProperties.gracePeriodDays()));
        tenantRepository.save(tenant);
        return invoice;
    }

    /** Daily job step 2: PAST_DUE tenants whose grace period has run out get hard-locked. */
    @Transactional
    public void sweepExpiredGracePeriods() {
        OffsetDateTime now = OffsetDateTime.now();
        for (Tenant tenant : tenantRepository.findAllByPlatformFalseAndSubscriptionStatusAndGracePeriodEndsAtBefore(
                SubscriptionStatus.PAST_DUE, now)) {
            tenant.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
            tenantRepository.save(tenant);
        }
    }

    /**
     * Idempotent on purpose - Razorpay retries webhook delivery, so a
     * duplicate call for an already-PAID invoice must be a no-op, not a
     * second period extension.
     */
    @Transactional
    public void handleWebhookPaid(String paymentLinkId) {
        SubscriptionInvoice invoice = invoiceRepository.findByRazorpayPaymentLinkId(paymentLinkId).orElse(null);
        if (invoice == null || invoice.getStatus() == SubscriptionInvoiceStatus.PAID) {
            return;
        }

        invoice.setStatus(SubscriptionInvoiceStatus.PAID);
        invoice.setPaidAt(OffsetDateTime.now());
        invoiceRepository.save(invoice);

        Tenant tenant = tenantRepository.findById(invoice.getTenantId()).orElse(null);
        if (tenant == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime base = tenant.getCurrentPeriodEnd().isAfter(now) ? tenant.getCurrentPeriodEnd() : now;
        tenant.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        tenant.setCurrentPeriodEnd(base.plusMonths(1));
        tenant.setGracePeriodEndsAt(null);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void adminOverride(UUID tenantId, SubscriptionStatus status, OffsetDateTime currentPeriodEnd) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        tenant.setSubscriptionStatus(status);
        if (currentPeriodEnd != null) {
            tenant.setCurrentPeriodEnd(currentPeriodEnd);
        }
        if (status != SubscriptionStatus.PAST_DUE) {
            tenant.setGracePeriodEndsAt(null);
        }
        tenantRepository.save(tenant);
    }
}
