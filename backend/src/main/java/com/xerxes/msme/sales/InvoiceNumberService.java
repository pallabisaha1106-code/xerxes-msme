package com.xerxes.msme.sales;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceNumberService {

    private static final String DISPLAY_FORMAT = "INV/%s/%06d";

    private final InvoiceNumberSequenceRepository sequenceRepository;

    public InvoiceNumberService(InvoiceNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Runs in its own transaction (REQUIRES_NEW) so the allocated number is
     * committed - and therefore never reused - even if the surrounding invoice
     * creation later rolls back for an unrelated reason (e.g. insufficient
     * stock on a later line item). A gap in the sequence is legally fine; a
     * duplicate number is not.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InvoiceNumberAllocation allocateNext(UUID tenantId, LocalDate invoiceDate) {
        String financialYear = FinancialYearResolver.resolve(invoiceDate);
        long number = sequenceRepository.allocateNext(tenantId, financialYear);
        String displayNumber = String.format(DISPLAY_FORMAT, financialYear, number);
        return new InvoiceNumberAllocation(financialYear, number, displayNumber);
    }
}
