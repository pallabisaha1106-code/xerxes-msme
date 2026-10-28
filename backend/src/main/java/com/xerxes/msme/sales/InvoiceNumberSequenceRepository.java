package com.xerxes.msme.sales;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceNumberSequenceRepository extends JpaRepository<InvoiceNumberSequence, InvoiceNumberSequenceId> {

    /**
     * Atomic upsert-and-increment: the first invoice of a financial year creates
     * the row at 1, every subsequent one increments it. The DB does the
     * read-modify-write itself (no app-side SELECT ... FOR UPDATE needed), so
     * concurrent requests for the same tenant/FY can never hand out the same
     * number - required for GST-compliant sequential invoice numbering.
     *
     * <p>Deliberately NOT annotated {@code @Modifying}: this INSERT carries a
     * RETURNING clause, so it must run through Hibernate's result-returning
     * query path (getSingleResult) rather than executeUpdate(), which would
     * discard the returned value.
     */
    @Query(
            value =
                    """
                    INSERT INTO invoice_number_sequences (tenant_id, financial_year, last_number)
                    VALUES (:tenantId, :financialYear, 1)
                    ON CONFLICT (tenant_id, financial_year)
                    DO UPDATE SET last_number = invoice_number_sequences.last_number + 1
                    RETURNING last_number
                    """,
            nativeQuery = true)
    long allocateNext(@Param("tenantId") UUID tenantId, @Param("financialYear") String financialYear);
}
