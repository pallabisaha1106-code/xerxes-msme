package com.xerxes.msme.inventory;

import com.xerxes.msme.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Append-only. Never update or delete a row - corrections are new rows (e.g. ADJUSTMENT). */
@Entity
@Table(name = "stock_ledger")
@Getter
@Setter
public class StockLedger extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "variant_id", nullable = false, updatable = false)
    private UUID variantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false)
    private MovementType movementType;

    @Column(name = "quantity_change", nullable = false, updatable = false)
    private BigDecimal quantityChange;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(updatable = false)
    private String remarks;

    @Column(name = "movement_date", nullable = false, updatable = false)
    private LocalDate movementDate = LocalDate.now();

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
}
