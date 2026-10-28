package com.xerxes.msme.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Category;
import com.xerxes.msme.catalog.CategoryRepository;
import com.xerxes.msme.catalog.ProductService;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.catalog.dto.ProductRequest;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.catalog.dto.VariantResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StarterCatalogServiceTest {

    @Mock private DefaultProductRepository defaultProductRepository;
    @Mock private DefaultProductCategoryRepository defaultProductCategoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductService productService;

    private StarterCatalogService starterCatalogService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        starterCatalogService = new StarterCatalogService(
                defaultProductRepository, defaultProductCategoryRepository, categoryRepository, variantRepository, productService);
        tenantId = UUID.randomUUID();
    }

    private DefaultProduct defaultProduct(String name, UUID categoryId) {
        DefaultProduct product = new DefaultProduct();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setDefaultProductCategoryId(categoryId);
        return product;
    }

    private DefaultProductCategory defaultCategory(UUID id, String name) {
        DefaultProductCategory category = new DefaultProductCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    @Test
    void createStarterProducts_createsOneProductPerDefaultProductWithZeroPriceAndStock() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        when(defaultProductRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(eq(tenantId), any())).thenReturn(false);
        when(productService.createProduct(eq(tenantId), any())).thenReturn(null);

        starterCatalogService.createStarterProducts(tenantId, List.of(rice.getId()));

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).createProduct(eq(tenantId), captor.capture());
        ProductRequest request = captor.getValue();
        assertThat(request.name()).isEqualTo("Kohinoor Basmati Rice");
        assertThat(request.categoryId()).isNotNull();
        assertThat(request.variants()).hasSize(1);
        assertThat(request.variants().get(0).sellingPrice().signum()).isZero();
        assertThat(request.variants().get(0).purchasePrice().signum()).isZero();
        assertThat(request.variants().get(0).sku()).isEqualTo("KOHINOOR-BASMATI-RICE");
    }

    @Test
    void createStarterProducts_noExistingTenantCategory_createsOneNamedAfterTheDefaultCategory() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        when(defaultProductRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(eq(tenantId), any())).thenReturn(false);

        starterCatalogService.createStarterProducts(tenantId, List.of(rice.getId()));

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("Rice");
        assertThat(categoryCaptor.getValue().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void createStarterProducts_existingTenantCategoryWithSameName_reusesItInsteadOfCreatingAnother() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        UUID existingCategoryId = UUID.randomUUID();
        Category existingCategory = new Category();
        existingCategory.setId(existingCategoryId);
        existingCategory.setTenantId(tenantId);
        existingCategory.setName("Rice");
        when(defaultProductRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.of(existingCategory));
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(eq(tenantId), any())).thenReturn(false);

        starterCatalogService.createStarterProducts(tenantId, List.of(rice.getId()));

        verify(categoryRepository, never()).save(any());
        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).createProduct(eq(tenantId), captor.capture());
        assertThat(captor.getValue().categoryId()).isEqualTo(existingCategoryId);
    }

    @Test
    void createStarterProducts_multipleProductsInSameDefaultCategory_createsTheTenantCategoryOnlyOnce() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice1 = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        DefaultProduct rice2 = defaultProduct("India Gate Rice", riceCategoryId);
        when(defaultProductRepository.findById(rice1.getId())).thenReturn(Optional.of(rice1));
        when(defaultProductRepository.findById(rice2.getId())).thenReturn(Optional.of(rice2));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(eq(tenantId), any())).thenReturn(false);

        starterCatalogService.createStarterProducts(tenantId, List.of(rice1.getId(), rice2.getId()));

        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(defaultProductCategoryRepository, times(1)).findById(riceCategoryId);
        verify(productService, times(2)).createProduct(eq(tenantId), any());
    }

    @Test
    void createStarterProducts_skuCollision_appendsIncrementingSuffix() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        when(defaultProductRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "KOHINOOR-BASMATI-RICE")).thenReturn(true);
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, "KOHINOOR-BASMATI-RICE-2")).thenReturn(false);
        when(productService.createProduct(eq(tenantId), any())).thenReturn((ProductResponse) null);

        starterCatalogService.createStarterProducts(tenantId, List.of(rice.getId()));

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).createProduct(eq(tenantId), captor.capture());
        assertThat(captor.getValue().variants().get(0).sku()).isEqualTo("KOHINOOR-BASMATI-RICE-2");
    }

    @Test
    void createStarterProducts_defaultProductHasImage_setsStarterImagePathOnTheCreatedVariant() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        rice.setImagePath("default-catalog/rice-category/abc.jpg");
        when(defaultProductRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(eq(tenantId), any())).thenReturn(false);

        UUID variantId = UUID.randomUUID();
        VariantResponse variantResponse = new VariantResponse(
                variantId,
                "KOHINOOR-BASMATI-RICE",
                null,
                Map.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                "PCS",
                null,
                null,
                "PCS",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
        ProductResponse productResponse = new ProductResponse(
                UUID.randomUUID(),
                "Kohinoor Basmati Rice",
                null,
                null,
                UUID.randomUUID(),
                null,
                BigDecimal.ZERO,
                "PCS",
                null,
                null,
                null,
                List.of(variantResponse));
        when(productService.createProduct(eq(tenantId), any())).thenReturn(productResponse);
        ProductVariant createdVariant = new ProductVariant();
        createdVariant.setId(variantId);
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(createdVariant));

        starterCatalogService.createStarterProducts(tenantId, List.of(rice.getId()));

        ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(variantCaptor.capture());
        assertThat(variantCaptor.getValue().getStarterImagePath()).isEqualTo("default-catalog/rice-category/abc.jpg");
    }

    @Test
    void createStarterProducts_defaultProductHasNoImage_neverTouchesTheVariantAfterCreation() {
        UUID riceCategoryId = UUID.randomUUID();
        DefaultProduct rice = defaultProduct("Kohinoor Basmati Rice", riceCategoryId);
        when(defaultProductRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(defaultProductCategoryRepository.findById(riceCategoryId))
                .thenReturn(Optional.of(defaultCategory(riceCategoryId, "Rice")));
        when(categoryRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Rice")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(variantRepository.existsByTenantIdAndSkuIgnoreCase(eq(tenantId), any())).thenReturn(false);
        when(productService.createProduct(eq(tenantId), any())).thenReturn(null);

        starterCatalogService.createStarterProducts(tenantId, List.of(rice.getId()));

        verify(variantRepository, never()).findById(any());
        verify(variantRepository, never()).save(any(ProductVariant.class));
    }

    @Test
    void createStarterProducts_unknownDefaultProduct_throwsResourceNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(defaultProductRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> starterCatalogService.createStarterProducts(tenantId, List.of(unknownId)));
    }

    @Test
    void createStarterProducts_defaultProductReferencesMissingCategory_throwsResourceNotFound() {
        UUID missingCategoryId = UUID.randomUUID();
        DefaultProduct product = defaultProduct("Orphan Product", missingCategoryId);
        when(defaultProductRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(defaultProductCategoryRepository.findById(missingCategoryId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> starterCatalogService.createStarterProducts(tenantId, List.of(product.getId())));
    }
}
