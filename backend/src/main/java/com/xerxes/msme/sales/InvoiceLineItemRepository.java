package com.xerxes.msme.sales;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    List<InvoiceLineItem> findAllByInvoiceId(UUID invoiceId);

    // Native query: invoice_line_items and invoices are linked by a plain
    // invoice_id UUID column, not a JPA association (this codebase avoids
    // entity relationships in favor of explicit repository lookups - see
    // the append-only/audit-trail design notes on StockService), so there's
    // no JPQL entity join available here.
    @Query(
            value =
                    """
                    select li.variant_id as variantId, sum(li.quantity) as totalQuantity, sum(li.line_total) as totalRevenue
                    from invoice_line_items li
                    join invoices i on i.id = li.invoice_id
                    where li.tenant_id = :tenantId and i.invoice_date >= :since
                    group by li.variant_id
                    order by sum(li.quantity) desc
                    limit 5
                    """,
            nativeQuery = true)
    List<VariantSalesAggregate> findTopSellingVariants(@Param("tenantId") UUID tenantId, @Param("since") LocalDate since);
}
