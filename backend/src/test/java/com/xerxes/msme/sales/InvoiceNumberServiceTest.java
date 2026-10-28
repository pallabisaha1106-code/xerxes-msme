package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberServiceTest {

    @Mock private InvoiceNumberSequenceRepository sequenceRepository;

    @Test
    void allocateNext_formatsDisplayNumberFromResolvedFinancialYearAndAllocatedNumber() {
        InvoiceNumberService service = new InvoiceNumberService(sequenceRepository);
        UUID tenantId = UUID.randomUUID();
        when(sequenceRepository.allocateNext(tenantId, "2026-27")).thenReturn(42L);

        InvoiceNumberAllocation allocation = service.allocateNext(tenantId, LocalDate.of(2026, 6, 15));

        assertThat(allocation.financialYear()).isEqualTo("2026-27");
        assertThat(allocation.invoiceNumber()).isEqualTo(42L);
        assertThat(allocation.displayNumber()).isEqualTo("INV/2026-27/000042");
    }

    @Test
    void allocateNext_padsSmallNumbersToSixDigits() {
        InvoiceNumberService service = new InvoiceNumberService(sequenceRepository);
        UUID tenantId = UUID.randomUUID();
        when(sequenceRepository.allocateNext(tenantId, "2025-26")).thenReturn(1L);

        InvoiceNumberAllocation allocation = service.allocateNext(tenantId, LocalDate.of(2025, 5, 1));

        assertThat(allocation.displayNumber()).isEqualTo("INV/2025-26/000001");
    }
}
