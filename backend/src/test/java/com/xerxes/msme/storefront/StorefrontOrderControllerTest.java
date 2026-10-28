package com.xerxes.msme.storefront;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.storefront.dto.RejectOrderRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
import com.xerxes.msme.user.Role;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = StorefrontOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorefrontOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StorefrontOrderService orderService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private StorefrontOrderResponse response(StorefrontOrderStatus status) {
        return new StorefrontOrderResponse(
                UUID.randomUUID(), status, "Jane Doe", "9876543210", null, null, null, null, null, null, null,
                new BigDecimal("299.00"), null, null, null, List.of());
    }

    @Test
    void list_noStatusParam_returnsAllOrders() throws Exception {
        when(orderService.list(tenantId, null)).thenReturn(List.of(response(StorefrontOrderStatus.NEW)));

        mockMvc.perform(get("/api/storefront/orders")).andExpect(status().isOk());
    }

    @Test
    void list_withStatusParam_filtersByStatus() throws Exception {
        when(orderService.list(tenantId, StorefrontOrderStatus.CONFIRMED)).thenReturn(List.of());

        mockMvc.perform(get("/api/storefront/orders").param("status", "CONFIRMED")).andExpect(status().isOk());
    }

    @Test
    void get_returnsOrderById() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.get(tenantId, orderId)).thenReturn(response(StorefrontOrderStatus.NEW));

        mockMvc.perform(get("/api/storefront/orders/" + orderId)).andExpect(status().isOk());
    }

    @Test
    void confirm_returnsConfirmedOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.confirm(tenantId, orderId, userId)).thenReturn(response(StorefrontOrderStatus.CONFIRMED));

        mockMvc.perform(post("/api/storefront/orders/" + orderId + "/confirm")).andExpect(status().isOk());
    }

    @Test
    void reject_withReasonBody_passesReasonThrough() throws Exception {
        UUID orderId = UUID.randomUUID();
        RejectOrderRequest request = new RejectOrderRequest("Out of stock");
        when(orderService.reject(tenantId, orderId, "Out of stock")).thenReturn(response(StorefrontOrderStatus.REJECTED));

        mockMvc.perform(post("/api/storefront/orders/" + orderId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void reject_withoutBody_passesNullReason() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.reject(eq(tenantId), eq(orderId), isNull())).thenReturn(response(StorefrontOrderStatus.REJECTED));

        mockMvc.perform(post("/api/storefront/orders/" + orderId + "/reject")).andExpect(status().isOk());
    }
}
