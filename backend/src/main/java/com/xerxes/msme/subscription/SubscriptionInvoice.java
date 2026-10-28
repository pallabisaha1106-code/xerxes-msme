package com.xerxes.msme.subscription;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** One billing period's charge for a tenant - a generated Razorpay Payment Link and its outcome. */
@Entity
@Table(name = "subscription_invoices")
@Getter
@Setter
public class SubscriptionInvoice extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionInvoiceStatus status;

    @Column(name = "razorpay_payment_link_id")
    private String razorpayPaymentLinkId;

    @Column(name = "razorpay_payment_link_url")
    private String razorpayPaymentLinkUrl;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;
}
