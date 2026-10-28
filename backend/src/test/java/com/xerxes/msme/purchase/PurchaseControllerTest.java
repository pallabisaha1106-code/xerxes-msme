package com.xerxes.msme.purchase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.purchase.dto.PurchaseLineItemRequest;
import com.xerxes.msme.purchase.dto.PurchaseRequest;
import com.xerxes.msme.purchase.dto.PurchaseResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
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

@WebMvcTest(controllers = PurchaseController.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PurchaseService purchaseService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private PurchaseRequest validRequest() {
        return new PurchaseRequest(
                UUID.randomUUID(), "REF-1", LocalDate.of(2026, 6, 1),
                List.of(new PurchaseLineItemRequest(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("100.00"))));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        PurchaseResponse response = new PurchaseResponse(
                UUID.randomUUID(), UUID.randomUUID(), "REF-1", LocalDate.of(2026, 6, 1), new BigDecimal("1000.00"),
                new BigDecimal("120.00"), new BigDecimal("1120.00"), List.of());
        when(purchaseService.createPurchase(eq(tenantId), eq(userId), any())).thenReturn(response);

        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_emptyLineItems_returns400() throws Exception {
        PurchaseRequest request = new PurchaseRequest(UUID.randomUUID(), null, LocalDate.now(), List.of());

        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_zeroQuantity_returns400() throws Exception {
        PurchaseRequest request = new PurchaseRequest(
                UUID.randomUUID(), null, LocalDate.now(),
                List.of(new PurchaseLineItemRequest(UUID.randomUUID(), BigDecimal.ZERO, BigDecimal.TEN)));

        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsPurchasesForCurrentTenant() throws Exception {
        when(purchaseService.listPurchases(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/purchases")).andExpect(status().isOk());
    }
}
