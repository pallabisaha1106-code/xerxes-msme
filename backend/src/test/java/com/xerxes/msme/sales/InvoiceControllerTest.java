package com.xerxes.msme.sales;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.sales.dto.InvoiceLineItemRequest;
import com.xerxes.msme.sales.dto.InvoiceRequest;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.user.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private InvoiceService invoiceService;
    @MockBean private InvoicePdfService invoicePdfService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private InvoiceRequest validRequest() {
        return new InvoiceRequest(
                UUID.randomUUID(), LocalDate.of(2026, 6, 20), null,
                List.of(new InvoiceLineItemRequest(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("299.00"))));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        InvoiceResponse response = new InvoiceResponse(
                UUID.randomUUID(), UUID.randomUUID(), "INV/2026-27/000001", LocalDate.of(2026, 6, 20), "27",
                new BigDecimal("299.00"), new BigDecimal("17.94"), new BigDecimal("17.94"), BigDecimal.ZERO,
                new BigDecimal("334.88"), InvoiceStatus.FINAL, List.of());
        when(invoiceService.createInvoice(eq(tenantId), eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_emptyLineItems_returns400() throws Exception {
        InvoiceRequest request = new InvoiceRequest(UUID.randomUUID(), LocalDate.now(), null, List.of());

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_negativeQuantity_returns400() throws Exception {
        InvoiceRequest request = new InvoiceRequest(
                UUID.randomUUID(), LocalDate.now(), null,
                List.of(new InvoiceLineItemRequest(UUID.randomUUID(), new BigDecimal("-1"), BigDecimal.TEN)));

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsInvoicesForCurrentTenant() throws Exception {
        when(invoiceService.listInvoices(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/invoices")).andExpect(status().isOk());
    }

    @Test
    void downloadPdf_returnsPdfWithAttachmentHeader() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        byte[] fakePdfBytes = "%PDF-1.4 fake".getBytes();
        when(invoicePdfService.generateInvoicePdf(tenantId, invoiceId))
                .thenReturn(new InvoicePdfService.InvoicePdf("INV-2026-27-000001.pdf", fakePdfBytes));

        mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", "attachment; filename=\"INV-2026-27-000001.pdf\""));
    }
}
