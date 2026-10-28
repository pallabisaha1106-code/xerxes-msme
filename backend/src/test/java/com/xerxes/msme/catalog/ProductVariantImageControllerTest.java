package com.xerxes.msme.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductVariantImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductVariantImageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductImageService productImageService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void upload_validImage_returns201() throws Exception {
        ProductVariantImageResponse response = new ProductVariantImageResponse(
                UUID.randomUUID(), "/media/x/products/y/a.jpg", "/media/x/products/y/a-480.jpg", 960, 1280, 0);
        when(productImageService.upload(eq(tenantId), eq(productId), eq(variantId), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/products/" + productId + "/variants/" + variantId + "/images").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.width").value(960));
    }

    @Test
    void list_returnsImagesForVariant() throws Exception {
        when(productImageService.list(tenantId, productId, variantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/products/" + productId + "/variants/" + variantId + "/images"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_delegatesToService() throws Exception {
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/products/" + productId + "/variants/" + variantId + "/images/" + imageId))
                .andExpect(status().isNoContent());
    }
}
