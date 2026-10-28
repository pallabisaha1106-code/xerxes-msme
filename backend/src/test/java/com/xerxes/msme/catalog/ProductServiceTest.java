package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.dto.ProductRequest;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.catalog.dto.ProductUpdateRequest;
import com.xerxes.msme.catalog.dto.VariantRequest;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.inventory.StockLedgerRepository;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.tenant.MembershipType;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private StockLedgerRepository stockLedgerRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private TenantRepository tenantRepository;

    private ProductService productService;
    private UUID tenantId;

    private static Tenant tenantOnPlan(MembershipType plan) {
        Tenant tenant = new Tenant();
        tenant.setSubscriptionPlan(plan);
        return tenant;
    }

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository, variantRepository, stockLedgerRepository, imageStorageService, tenantRepository);
        tenantId = UUID.randomUUID();
        // Generous default so every existing test (not specifically exercising the
        // plan limit) doesn't need its own stub - countByTenantId's Mockito default
        // of 0 stays well under PREMIUM's 500.
        lenient().when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantOnPlan(MembershipType.PREMIUM)));
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });
        lenient().when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId(UUID.randomUUID());
            }
            return v;
        });
    }

    private ProductRequest requestWithOneVariant(String sku) {
        return new ProductRequest(
                "Cotton T-Shirt", "BrandX", null, null, "6109", new BigDecimal("12"), "PCS", null, null, null,
                List.of(new VariantRequest(
                        sku, null, Map.of("size", "M"), new BigDecimal("150"), new BigDecimal("299"), null, null,
                        null, null)));
    }

    @Test
    void createProduct_savesProductAndAllVariants_resolvingEffectiveGstFromProductDefault() {
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(false);

        ProductResponse response = productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M"));

        assertThat(response.variants()).hasSize(1);
        assertThat(response.variants().get(0).sku()).isEqualTo("TSHIRT-M");
        assertThat(response.variants().get(0).effectiveGstRate()).isEqualByComparingTo("12");
        assertThat(response.variants().get(0).attributes()).containsEntry("size", "M");
    }

    @Test
    void createProduct_duplicateSkuWithinTenant_throwsAndNeverSavesProduct() {
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M")));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_nullReorderThreshold_defaultsToZero() {
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(false);

        ProductResponse response = productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M"));

        assertThat(response.variants().get(0).reorderThreshold()).isEqualByComparingTo("0");
    }

    @Test
    void createProduct_freePlanUnderLimit_succeeds() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantOnPlan(MembershipType.FREE)));
        when(productRepository.countByTenantId(tenantId)).thenReturn(9L);
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(false);

        ProductResponse response = productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M"));

        assertThat(response.variants()).hasSize(1);
    }

    @Test
    void createProduct_freePlanAtLimit_throwsBusinessRuleViolation() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantOnPlan(MembershipType.FREE)));
        when(productRepository.countByTenantId(tenantId)).thenReturn(10L);

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M")));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_plusPlanUnderLimit_succeeds() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantOnPlan(MembershipType.PLUS)));
        when(productRepository.countByTenantId(tenantId)).thenReturn(99L);
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(false);

        ProductResponse response = productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M"));

        assertThat(response.variants()).hasSize(1);
    }

    @Test
    void createProduct_plusPlanAtLimit_throwsBusinessRuleViolation() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantOnPlan(MembershipType.PLUS)));
        when(productRepository.countByTenantId(tenantId)).thenReturn(100L);

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M")));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_unknownTenant_throwsResourceNotFound() {
        UUID unknownTenantId = UUID.randomUUID();
        when(tenantRepository.findById(unknownTenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.createProduct(unknownTenantId, requestWithOneVariant("TSHIRT-M")));
    }

    @Test
    void getProduct_unknownId_throwsResourceNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(tenantId, productId));
    }

    @Test
    void getProduct_found_includesItsVariants() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setDefaultGstRate(BigDecimal.ZERO);
        product.setDefaultUom("PCS");

        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setSku("SKU-X");

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant));

        ProductResponse response = productService.getProduct(tenantId, productId);

        assertThat(response.variants()).extracting("sku").containsExactly("SKU-X");
    }

    @Test
    void updateProduct_savesNewFieldsIncludingFabricCareAndIncludedItems() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setName("Old Name");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of());

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Cotton T-Shirt", "BrandY", "Updated description", null, "6109", new BigDecimal("5"), "PCS",
                "100% Cotton", "Machine wash cold", "1 top, 1 dupatta");

        ProductResponse response = productService.updateProduct(tenantId, productId, request);

        assertThat(response.name()).isEqualTo("Cotton T-Shirt");
        assertThat(response.fabric()).isEqualTo("100% Cotton");
        assertThat(response.careInstructions()).isEqualTo("Machine wash cold");
        assertThat(response.includedItems()).isEqualTo("1 top, 1 dupatta");
        assertThat(product.getName()).isEqualTo("Cotton T-Shirt");
    }

    @Test
    void updateProduct_unknownProduct_throwsResourceNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        ProductUpdateRequest request = new ProductUpdateRequest(
                "Cotton T-Shirt", null, null, null, null, BigDecimal.ZERO, "PCS", null, null, null);

        assertThrows(
                ResourceNotFoundException.class, () -> productService.updateProduct(tenantId, productId, request));
    }

    @Test
    void listProducts_returnsEachProductWithItsVariants() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setDefaultGstRate(BigDecimal.ZERO);
        product.setDefaultUom("PCS");
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllByProductId(product.getId())).thenReturn(List.of());

        List<ProductResponse> responses = productService.listProducts(tenantId);

        assertThat(responses).hasSize(1);
    }

    private VariantRequest variantRequest(String sku) {
        return new VariantRequest(
                sku, null, Map.of("size", "L"), new BigDecimal("150"), new BigDecimal("299"), null, null, null, null);
    }

    private Product productWithId(UUID productId) {
        Product product = new Product();
        product.setId(productId);
        product.setDefaultGstRate(BigDecimal.ZERO);
        product.setDefaultUom("PCS");
        return product;
    }

    private ProductVariant existingVariant(UUID productId, UUID variantId, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku(sku);
        return variant;
    }

    @Test
    void addVariant_newSku_savesAndReturnsUpdatedProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "NEW-SKU")).thenReturn(false);
        when(variantRepository.findAllByProductId(productId))
                .thenReturn(List.of(existingVariant(productId, UUID.randomUUID(), "NEW-SKU")));

        ProductResponse response = productService.addVariant(tenantId, productId, variantRequest("NEW-SKU"));

        assertThat(response.variants()).extracting("sku").containsExactly("NEW-SKU");
        verify(variantRepository).save(any(ProductVariant.class));
    }

    @Test
    void addVariant_duplicateSku_throwsAndNeverSaves() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "DUP-SKU")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.addVariant(tenantId, productId, variantRequest("DUP-SKU")));

        verify(variantRepository, never()).save(any());
    }

    @Test
    void addVariant_unknownProduct_throwsResourceNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.addVariant(tenantId, productId, variantRequest("ANY-SKU")));
    }

    @Test
    void updateVariant_changesFields_savesAndReturnsUpdatedProduct() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "OLD-SKU");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "NEW-SKU")).thenReturn(false);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant));

        ProductResponse response = productService.updateVariant(tenantId, productId, variantId, variantRequest("NEW-SKU"));

        assertThat(variant.getSku()).isEqualTo("NEW-SKU");
        assertThat(response.variants()).hasSize(1);
    }

    @Test
    void updateVariant_skuUnchanged_doesNotCheckForDuplicates() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SAME-SKU");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant));

        productService.updateVariant(tenantId, productId, variantId, variantRequest("SAME-SKU"));

        verify(variantRepository, never()).existsByTenantIdAndSkuIgnoreCase(any(), any());
    }

    @Test
    void updateVariant_skuChangedToOneAlreadyUsed_throwsDuplicate() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "OLD-SKU");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TAKEN-SKU")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.updateVariant(tenantId, productId, variantId, variantRequest("TAKEN-SKU")));
    }

    @Test
    void updateVariant_variantBelongsToDifferentProduct_throwsResourceNotFound() {
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(otherProductId, variantId, "OLD-SKU");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateVariant(tenantId, productId, variantId, variantRequest("OLD-SKU")));
    }

    @Test
    void deleteVariant_noStockHistoryAndOtherVariantsRemain_hardDeletes() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SKU-1");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(stockLedgerRepository.existsByTenantIdAndVariantId(tenantId, variantId)).thenReturn(false);
        when(variantRepository.countByProductId(productId)).thenReturn(2L);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of());

        productService.deleteVariant(tenantId, productId, variantId);

        verify(variantRepository).delete(variant);
        verify(variantRepository, never()).save(any());
        verify(imageStorageService).deleteAllUnderPrefix(tenantId + "/products/" + variantId + "/");
        verify(imageStorageService).deleteAllUnderPrefix(tenantId + "/product-originals/" + variantId + "/");
    }

    @Test
    void deleteVariant_hasStockHistory_deactivatesInsteadOfDeleting() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SKU-1");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(stockLedgerRepository.existsByTenantIdAndVariantId(tenantId, variantId)).thenReturn(true);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant));

        productService.deleteVariant(tenantId, productId, variantId);

        assertThat(variant.isActive()).isFalse();
        verify(variantRepository).save(variant);
        verify(variantRepository, never()).delete(any());
        verify(imageStorageService, never()).deleteAllUnderPrefix(any());
    }

    @Test
    void deleteVariant_onlyVariantOfProductWithNoHistory_throwsBusinessRuleViolation() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SKU-1");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(stockLedgerRepository.existsByTenantIdAndVariantId(tenantId, variantId)).thenReturn(false);
        when(variantRepository.countByProductId(productId)).thenReturn(1L);

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productService.deleteVariant(tenantId, productId, variantId));

        verify(variantRepository, never()).delete(any());
    }

    @Test
    void deleteVariant_unknownVariant_throwsResourceNotFound() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> productService.deleteVariant(tenantId, productId, variantId));
    }

    private VariantRequest variantRequest(String sku, String barcode) {
        return new VariantRequest(
                sku, barcode, Map.of("size", "L"), new BigDecimal("150"), new BigDecimal("299"), null, null, null,
                null);
    }

    @Test
    void createProduct_blankBarcode_generatesTwelveDigitBarcode() {
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(false);
        when(variantRepository.existsByTenantIdAndBarcode(eq(tenantId), any())).thenReturn(false);

        ProductResponse response = productService.createProduct(tenantId, requestWithOneVariant("TSHIRT-M"));

        String barcode = response.variants().get(0).barcode();
        assertThat(barcode).matches("\\d{12}");
    }

    @Test
    void createProduct_explicitBarcodeAlreadyUsed_throwsDuplicate() {
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "TSHIRT-M")).thenReturn(false);
        when(variantRepository.existsByTenantIdAndBarcode(tenantId, "123456789012")).thenReturn(true);

        ProductRequest request = new ProductRequest(
                "Cotton T-Shirt", "BrandX", null, null, "6109", new BigDecimal("12"), "PCS", null, null, null,
                List.of(variantRequest("TSHIRT-M", "123456789012")));

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(tenantId, request));
    }

    @Test
    void addVariant_blankBarcode_generatesBarcode() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "NEW-SKU")).thenReturn(false);
        when(variantRepository.existsByTenantIdAndBarcode(eq(tenantId), any())).thenReturn(false);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of());

        productService.addVariant(tenantId, productId, variantRequest("NEW-SKU", null));

        org.mockito.ArgumentCaptor<ProductVariant> captor = org.mockito.ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(captor.capture());
        assertThat(captor.getValue().getBarcode()).matches("\\d{12}");
    }

    @Test
    void updateVariant_blankBarcodeWithExistingBarcode_keepsExistingBarcode() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SKU-1");
        variant.setBarcode("111122223333");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant));

        productService.updateVariant(tenantId, productId, variantId, variantRequest("SKU-1", null));

        assertThat(variant.getBarcode()).isEqualTo("111122223333");
        verify(variantRepository, never()).existsByTenantIdAndBarcode(any(), any());
    }

    @Test
    void updateVariant_blankBarcodeWithNoExistingBarcode_generatesOne() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SKU-1");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByTenantIdAndBarcode(eq(tenantId), any())).thenReturn(false);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant));

        productService.updateVariant(tenantId, productId, variantId, variantRequest("SKU-1", null));

        assertThat(variant.getBarcode()).matches("\\d{12}");
    }

    @Test
    void updateVariant_changingToBarcodeAlreadyUsedByAnother_throwsDuplicate() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = existingVariant(productId, variantId, "SKU-1");
        variant.setBarcode("111122223333");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(productWithId(productId)));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByTenantIdAndBarcode(tenantId, "999988887777")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> productService.updateVariant(tenantId, productId, variantId, variantRequest("SKU-1", "999988887777")));
    }

    @Test
    void findVariantByBarcode_activeVariantFound_returnsScanResponse() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Product product = productWithId(productId);
        product.setName("Cotton T-Shirt");

        ProductVariant variant = existingVariant(productId, variantId, "TSHIRT-M");
        variant.setBarcode("123456789012");
        variant.setSellingPrice(new BigDecimal("499"));
        variant.setCurrentStock(new BigDecimal("10"));

        when(variantRepository.findByTenantIdAndBarcodeAndActiveTrue(tenantId, "123456789012"))
                .thenReturn(Optional.of(variant));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        var response = productService.findVariantByBarcode(tenantId, "123456789012");

        assertThat(response.productName()).isEqualTo("Cotton T-Shirt");
        assertThat(response.sku()).isEqualTo("TSHIRT-M");
        assertThat(response.sellingPrice()).isEqualByComparingTo("499");
        assertThat(response.currentStock()).isEqualByComparingTo("10");
    }

    @Test
    void findVariantByBarcode_notFoundOrInactive_throwsResourceNotFound() {
        when(variantRepository.findByTenantIdAndBarcodeAndActiveTrue(tenantId, "000000000000"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.findVariantByBarcode(tenantId, "000000000000"));
    }
}
