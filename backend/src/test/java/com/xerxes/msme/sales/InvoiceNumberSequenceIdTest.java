package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoiceNumberSequenceIdTest {

    @Test
    void equalsAndHashCode_dependOnBothFields() {
        UUID tenantId = UUID.randomUUID();
        InvoiceNumberSequenceId a = new InvoiceNumberSequenceId(tenantId, "2025-26");
        InvoiceNumberSequenceId b = new InvoiceNumberSequenceId(tenantId, "2025-26");
        InvoiceNumberSequenceId differentYear = new InvoiceNumberSequenceId(tenantId, "2026-27");
        InvoiceNumberSequenceId differentTenant = new InvoiceNumberSequenceId(UUID.randomUUID(), "2025-26");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(differentYear);
        assertThat(a).isNotEqualTo(differentTenant);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not an id");
        assertThat(a).isEqualTo(a);
    }

    @Test
    void noArgsConstructor_allowsSettersForJpa() {
        InvoiceNumberSequenceId id = new InvoiceNumberSequenceId();
        UUID tenantId = UUID.randomUUID();
        id.setTenantId(tenantId);
        id.setFinancialYear("2025-26");

        assertThat(id.getTenantId()).isEqualTo(tenantId);
        assertThat(id.getFinancialYear()).isEqualTo("2025-26");
    }
}
