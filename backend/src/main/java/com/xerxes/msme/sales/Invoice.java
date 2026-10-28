package com.xerxes.msme.sales;

import com.xerxes.msme.common.MutableEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice extends MutableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "financial_year", nullable = false, updatable = false)
    private String financialYear;

    @Column(name = "invoice_number", nullable = false, updatable = false)
    private long invoiceNumber;

    @Column(name = "invoice_display_number", nullable = false, updatable = false)
    private String invoiceDisplayNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "place_of_supply_state_code", nullable = false, length = 2)
    private String placeOfSupplyStateCode;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "cgst_amount", nullable = false)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", nullable = false)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.FINAL;

    @Column(name = "created_by")
    private UUID createdBy;
}
