package com.xerxes.msme.catalog;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.catalog.dto.ProductCapacityResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductCapacityController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductCapacityControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductCapacityService productCapacityService;
    @MockBean private AuthContext authContext;

    @Test
    void capacity_returnsTheCallingShopsRoom() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
        when(productCapacityService.capacity(tenantId)).thenReturn(new ProductCapacityResponse(100, 37, 63));

        mockMvc.perform(get("/api/products/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(100))
                .andExpect(jsonPath("$.used").value(37))
                .andExpect(jsonPath("$.remaining").value(63));
    }
}
