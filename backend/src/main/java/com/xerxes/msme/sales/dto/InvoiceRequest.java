package com.xerxes.msme.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
        @NotNull UUID customerId,
        @NotNull LocalDate invoiceDate,
        /** Defaults to the customer's own state code when omitted. */
        String placeOfSupplyStateCode,
        @NotEmpty @Valid List<InvoiceLineItemRequest> lineItems) {}
