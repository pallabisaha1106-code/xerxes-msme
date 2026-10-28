package com.xerxes.msme.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class InvoiceNumberSequenceId implements Serializable {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "financial_year")
    private String financialYear;

    public InvoiceNumberSequenceId(UUID tenantId, String financialYear) {
        this.tenantId = tenantId;
        this.financialYear = financialYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceNumberSequenceId that)) return false;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(financialYear, that.financialYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, financialYear);
    }
}
