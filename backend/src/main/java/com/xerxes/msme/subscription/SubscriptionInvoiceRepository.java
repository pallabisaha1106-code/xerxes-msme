package com.xerxes.msme.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, UUID> {

    Optional<SubscriptionInvoice> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);

    Optional<SubscriptionInvoice> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, SubscriptionInvoiceStatus status);

    boolean existsByTenantIdAndStatus(UUID tenantId, SubscriptionInvoiceStatus status);
}
