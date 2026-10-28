package com.xerxes.msme.sales;

import java.math.BigDecimal;
import java.util.UUID;

/** Spring Data JPA projection for {@link InvoiceLineItemRepository#findTopSellingVariants}. */
public interface VariantSalesAggregate {

    UUID getVariantId();

    BigDecimal getTotalQuantity();

    BigDecimal getTotalRevenue();
}
