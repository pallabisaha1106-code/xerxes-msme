package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminMarketplaceListingResponse;
import com.xerxes.msme.admin.dto.ReviewMarketplaceListingRequest;
import com.xerxes.msme.marketplace.MarketplaceListingStatus;
import com.xerxes.msme.marketplace.MarketplaceOrderMode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminMarketplaceListingController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminMarketplaceListingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminMarketplaceListingService adminMarketplaceListingService;

    private final UUID tenantId = UUID.randomUUID();

    private AdminMarketplaceListingResponse response(MarketplaceListingStatus status) {
        return new AdminMarketplaceListingResponse(
                tenantId, "Veera Collections", null, null, "Gujar Nagar", null, null, null, null, null, null, null,
                null, "919876543210", MarketplaceOrderMode.WHATSAPP, status, null, null, null);
    }

    @Test
    void list_noStatusParam_returnsAllListings() throws Exception {
        when(adminMarketplaceListingService.list(null)).thenReturn(List.of(response(MarketplaceListingStatus.PENDING)));

        mockMvc.perform(get("/api/admin/marketplace/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessName").value("Veera Collections"));
    }

    @Test
    void list_withStatusParam_filtersByStatus() throws Exception {
        when(adminMarketplaceListingService.list(MarketplaceListingStatus.LIVE)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/marketplace/listings").param("status", "LIVE")).andExpect(status().isOk());
    }

    @Test
    void approve_delegatesToService() throws Exception {
        when(adminMarketplaceListingService.approve(tenantId)).thenReturn(response(MarketplaceListingStatus.LIVE));

        mockMvc.perform(post("/api/admin/marketplace/listings/" + tenantId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void reject_withReasonBody_passesReasonThrough() throws Exception {
        ReviewMarketplaceListingRequest request = new ReviewMarketplaceListingRequest("Please add a clearer photo.");
        when(adminMarketplaceListingService.reject(tenantId, "Please add a clearer photo."))
                .thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(post("/api/admin/marketplace/listings/" + tenantId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void reject_withoutBody_passesNullReason() throws Exception {
        when(adminMarketplaceListingService.reject(eq(tenantId), isNull())).thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(post("/api/admin/marketplace/listings/" + tenantId + "/reject")).andExpect(status().isOk());
    }

    @Test
    void unpublish_withReasonBody_passesReasonThrough() throws Exception {
        ReviewMarketplaceListingRequest request = new ReviewMarketplaceListingRequest("Reported by another vendor.");
        when(adminMarketplaceListingService.unpublish(tenantId, "Reported by another vendor."))
                .thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(post("/api/admin/marketplace/listings/" + tenantId + "/unpublish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
