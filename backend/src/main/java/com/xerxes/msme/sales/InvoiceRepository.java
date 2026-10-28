package com.xerxes.msme.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findAllByTenantId(UUID tenantId);

    Optional<Invoice> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);

    List<Invoice> findTop5ByTenantIdOrderByInvoiceDateDescCreatedAtDesc(UUID tenantId);

    @Query("select sum(i.totalAmount) from Invoice i where i.tenantId = :tenantId and i.invoiceDate between :start and :end")
    BigDecimal sumTotalBetween(@Param("tenantId") UUID tenantId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
