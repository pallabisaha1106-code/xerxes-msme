package com.xerxes.msme.marketplace;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.marketplace.dto.CreateStarterProductsRequest;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
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

@WebMvcTest(controllers = StarterCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class StarterCatalogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StarterCatalogService starterCatalogService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void create_delegatesToServiceWithCurrentTenantId() throws Exception {
        UUID defaultProductId = UUID.randomUUID();
        CreateStarterProductsRequest request = new CreateStarterProductsRequest(List.of(defaultProductId));

        mockMvc.perform(post("/api/marketplace/starter-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(starterCatalogService).createStarterProducts(eq(tenantId), eq(List.of(defaultProductId)));
    }
}
