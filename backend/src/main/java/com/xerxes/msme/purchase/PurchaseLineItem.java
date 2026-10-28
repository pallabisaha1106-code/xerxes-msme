package com.xerxes.msme.purchase;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "purchase_line_items")
@Getter
@Setter
public class PurchaseLineItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "purchase_entry_id", nullable = false, updatable = false)
    private UUID purchaseEntryId;

    @Column(name = "variant_id", nullable = false, updatable = false)
    private UUID variantId;

    @Column(nullable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "gst_rate", nullable = false, updatable = false)
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, updatable = false)
    private BigDecimal lineTotal;
}
