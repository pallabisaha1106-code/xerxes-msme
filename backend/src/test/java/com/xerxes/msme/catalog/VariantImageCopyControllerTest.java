package com.xerxes.msme.catalog;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = VariantImageCopyController.class)
@AutoConfigureMockMvc(addFilters = false)
class VariantImageCopyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private VariantImageCopyService variantImageCopyService;
    @MockBean private AuthContext authContext;

    @Test
    void copy_returns201WithTheNewPhotos() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID targetVariantId = UUID.randomUUID();
        UUID sourceVariantId = UUID.randomUUID();
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
        when(variantImageCopyService.copyAll(tenantId, productId, sourceVariantId, targetVariantId))
                .thenReturn(List.of(new ProductVariantImageResponse(
                        UUID.randomUUID(), "/media/images/x.jpg", "/media/images/x-480.jpg", 960, 1200, 0)));

        mockMvc.perform(post("/api/products/" + productId + "/variants/" + targetVariantId + "/images/copy-from/"
                        + sourceVariantId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].width").value(960))
                .andExpect(jsonPath("$[0].displayOrder").value(0));
    }
}
