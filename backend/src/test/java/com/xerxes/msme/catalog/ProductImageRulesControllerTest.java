package com.xerxes.msme.catalog;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.catalog.dto.ProductImageRulesResponse;
import com.xerxes.msme.marketplace.ImageProfile;
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

@WebMvcTest(controllers = ProductImageRulesController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductImageRulesControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductImageService productImageService;
    @MockBean private AuthContext authContext;

    @Test
    void rules_returnsTheCallingShopsLimits() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
        when(productImageService.rules(tenantId))
                .thenReturn(new ProductImageRulesResponse(ImageProfile.COMPACT, 800, 800, 0.5, 2.0));

        mockMvc.perform(get("/api/product-image-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("COMPACT"))
                .andExpect(jsonPath("$.minWidth").value(800))
                .andExpect(jsonPath("$.maxAspectRatio").value(2.0));
    }
}
