package com.xerxes.msme.marketplace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MarketplaceListingController.class)
@AutoConfigureMockMvc(addFilters = false)
class MarketplaceListingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MarketplaceListingService listingService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private MarketplaceListingResponse response(MarketplaceListingStatus status) {
        return new MarketplaceListingResponse(
                UUID.randomUUID(), null, null, "Gujar Nagar", null, null, null, null, null, null, null, null,
                "919876543210", MarketplaceOrderMode.WHATSAPP, status, null, null);
    }

    @Test
    void get_returnsCurrentTenantsListing() throws Exception {
        when(listingService.getListing(tenantId)).thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(get("/api/marketplace/listing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void update_savesAndReturnsUpdatedListing() throws Exception {
        UpdateMarketplaceListingRequest request = new UpdateMarketplaceListingRequest(
                null, "Gujar Nagar", null, null, null, null, null, null, null, "9876543210", MarketplaceOrderMode.WHATSAPP);
        when(listingService.updateListing(eq(tenantId), any())).thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(put("/api/marketplace/listing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingOrderMode_returns400() throws Exception {
        String body = "{\"area\":\"Gujar Nagar\"}";

        mockMvc.perform(put("/api/marketplace/listing").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadPhoto_delegatesToServiceAndReturnsUpdatedListing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", "fake".getBytes());
        when(listingService.uploadPhoto(eq(tenantId), any())).thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(multipart("/api/marketplace/listing/photo").file(file)).andExpect(status().isOk());
    }

    @Test
    void deletePhoto_delegatesToService() throws Exception {
        when(listingService.deletePhoto(tenantId)).thenReturn(response(MarketplaceListingStatus.DRAFT));

        mockMvc.perform(delete("/api/marketplace/listing/photo")).andExpect(status().isOk());
    }

    @Test
    void submit_delegatesToServiceAndReturnsPending() throws Exception {
        when(listingService.submit(tenantId)).thenReturn(response(MarketplaceListingStatus.PENDING));

        mockMvc.perform(post("/api/marketplace/listing/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
