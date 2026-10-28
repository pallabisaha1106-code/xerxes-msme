package com.xerxes.msme.storefront;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.storefront.dto.PublicVariantResponse;
import com.xerxes.msme.storefront.dto.StorefrontOrderLineItemRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicStorefrontController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicStorefrontControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StorefrontCatalogService catalogService;
    @MockBean private StorefrontOrderService orderService;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void products_returnsCatalogForTenant() throws Exception {
        UUID variantId = UUID.randomUUID();
        PublicVariantResponse product = new PublicVariantResponse(
                UUID.randomUUID(), "Cotton T-Shirt", "desc", variantId, "TSHIRT-M", Map.of("size", "M"),
                new BigDecimal("299.00"), List.of(), true);
        when(catalogService.listAvailableProducts(tenantId)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/public/storefront/" + tenantId + "/products")).andExpect(status().isOk());
    }

    @Test
    void submitOrder_validRequest_returns201() throws Exception {
        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "Jane Doe", "9876543210", null, null, null, null, null, null, null,
                List.of(new StorefrontOrderLineItemRequest(UUID.randomUUID(), BigDecimal.ONE)));
        StorefrontOrderResponse response = new StorefrontOrderResponse(
                UUID.randomUUID(), StorefrontOrderStatus.NEW, "Jane Doe", "9876543210", null, null, null, null, null,
                null, null, new BigDecimal("299.00"), null, null, null, List.of());
        when(orderService.submit(eq(tenantId), any())).thenReturn(response);

        mockMvc.perform(post("/api/public/storefront/" + tenantId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void submitOrder_missingCustomerName_returns400() throws Exception {
        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "", "9876543210", null, null, null, null, null, null, null,
                List.of(new StorefrontOrderLineItemRequest(UUID.randomUUID(), BigDecimal.ONE)));

        mockMvc.perform(post("/api/public/storefront/" + tenantId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitOrder_emptyLineItems_returns400() throws Exception {
        StorefrontOrderRequest request = new StorefrontOrderRequest(
                "Jane Doe", "9876543210", null, null, null, null, null, null, null, List.of());

        mockMvc.perform(post("/api/public/storefront/" + tenantId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
