package com.xerxes.msme.storefront;

import com.xerxes.msme.common.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A guest visitor's order request, awaiting the shop owner's review. See package-info for the flow. */
@Entity
@Table(name = "storefront_orders")
@Getter
@Setter
public class StorefrontOrder extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorefrontOrderStatus status = StorefrontOrderStatus.NEW;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;

    private String pincode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "state_code", length = 2)
    private String stateCode;

    private String notes;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "confirmed_invoice_id")
    private UUID confirmedInvoiceId;
}
