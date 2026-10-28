package com.xerxes.msme.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.catalog.dto.ProductRequest;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.catalog.dto.VariantRequest;
import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProductService productService;
    @MockBean private AuthContext authContext;
    @MockBean private BulkImportSupport bulkImportSupport;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private ProductRequest validRequest() {
        return new ProductRequest(
                "Cotton T-Shirt", "BrandX", null, null, "6109", new BigDecimal("12"), "PCS", null, null, null,
                List.of(new VariantRequest(
                        "TSHIRT-M", null, Map.of("size", "M"), new BigDecimal("150"), new BigDecimal("299"), null,
                        null, null, null)));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        ProductResponse response =
                new ProductResponse(UUID.randomUUID(), "Cotton T-Shirt", "BrandX", null, null, "6109",
                        new BigDecimal("12"), "PCS", null, null, null, List.of());
        when(productService.createProduct(eq(tenantId), any())).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cotton T-Shirt"));
    }

    @Test
    void create_noVariants_returns400() throws Exception {
        ProductRequest request = new ProductRequest(
                "Cotton T-Shirt", "BrandX", null, null, "6109", new BigDecimal("12"), "PCS", null, null, null,
                List.of());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_gstRateOver100_returns400() throws Exception {
        ProductRequest request = new ProductRequest(
                "Cotton T-Shirt", "BrandX", null, null, "6109", new BigDecimal("150"), "PCS", null, null, null,
                List.of(new VariantRequest(
                        "TSHIRT-M", null, null, new BigDecimal("150"), new BigDecimal("299"), null, null, null,
                        null)));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsProductsForCurrentTenant() throws Exception {
        when(productService.listProducts(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void get_returnsProductById() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId, "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", null, null, null, List.of());
        when(productService.getProduct(tenantId, productId)).thenReturn(response);

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cotton T-Shirt"));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId, "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", "100% Cotton", null,
                null, List.of());
        when(productService.updateProduct(eq(tenantId), eq(productId), any())).thenReturn(response);

        var request = new com.xerxes.msme.catalog.dto.ProductUpdateRequest(
                "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", "100% Cotton", null, null);

        mockMvc.perform(put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fabric").value("100% Cotton"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bulkCreate_delegatesToBulkImportSupportWithTenantScopedCreator() throws Exception {
        BulkImportResult result = new BulkImportResult(1, 0, List.of());
        when(bulkImportSupport.run(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(validRequest()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1));

        ArgumentCaptor<Consumer<ProductRequest>> creatorCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(bulkImportSupport).run(any(), creatorCaptor.capture());

        ProductRequest request = validRequest();
        creatorCaptor.getValue().accept(request);
        verify(productService).createProduct(tenantId, request);
    }

    private VariantRequest validVariantRequest(String sku) {
        return new VariantRequest(
                sku, null, Map.of("size", "L"), new BigDecimal("150"), new BigDecimal("299"), null, null, null, null);
    }

    @Test
    void addVariant_validRequest_returns201() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId, "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", null, null, null, List.of());
        when(productService.addVariant(eq(tenantId), eq(productId), any())).thenReturn(response);

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validVariantRequest("TSHIRT-L"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cotton T-Shirt"));
    }

    @Test
    void addVariant_duplicateSku_returns409() throws Exception {
        UUID productId = UUID.randomUUID();
        when(productService.addVariant(eq(tenantId), eq(productId), any()))
                .thenThrow(new DuplicateResourceException("SKU already exists: TSHIRT-L"));

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validVariantRequest("TSHIRT-L"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateVariant_validRequest_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId, "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", null, null, null, List.of());
        when(productService.updateVariant(eq(tenantId), eq(productId), eq(variantId), any())).thenReturn(response);

        mockMvc.perform(put("/api/products/" + productId + "/variants/" + variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validVariantRequest("TSHIRT-L"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cotton T-Shirt"));
    }

    @Test
    void scanBarcode_found_returnsVariantScanResponse() throws Exception {
        var response = new com.xerxes.msme.catalog.dto.VariantScanResponse(
                UUID.randomUUID(), "Cotton T-Shirt", UUID.randomUUID(), "TSHIRT-M", "123456789012", Map.of("size", "M"),
                new BigDecimal("499"), new BigDecimal("5"), "6109", new BigDecimal("10"));
        when(productService.findVariantByBarcode(tenantId, "123456789012")).thenReturn(response);

        mockMvc.perform(get("/api/products/variants/by-barcode/123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TSHIRT-M"))
                .andExpect(jsonPath("$.sellingPrice").value(499));
    }

    @Test
    void deleteVariant_delegatesToServiceAndReturnsUpdatedProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId, "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", null, null, null, List.of());
        when(productService.deleteVariant(tenantId, productId, variantId)).thenReturn(response);

        mockMvc.perform(delete("/api/products/" + productId + "/variants/" + variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cotton T-Shirt"));
    }
}
