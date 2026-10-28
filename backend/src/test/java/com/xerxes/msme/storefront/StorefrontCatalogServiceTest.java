package com.xerxes.msme.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Category;
import com.xerxes.msme.catalog.CategoryRepository;
import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantImage;
import com.xerxes.msme.catalog.ProductVariantImageRepository;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.storefront.dto.PublicVariantResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorefrontCatalogServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductVariantImageRepository imageRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ImageStorageService imageStorageService;

    private StorefrontCatalogService catalogService;
    private UUID tenantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        catalogService = new StorefrontCatalogService(
                productRepository, variantRepository, imageRepository, categoryRepository, imageStorageService,
                new StorefrontProperties(null, true));
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Product product() {
        Product product = new Product();
        product.setId(productId);
        product.setName("Cotton T-Shirt");
        product.setDescription("A soft cotton tee");
        return product;
    }

    private ProductVariant variant(boolean active, BigDecimal stock) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProductId(productId);
        variant.setSku("TSHIRT-M");
        variant.setAttributes(Map.of("size", "M"));
        variant.setSellingPrice(new BigDecimal("299.00"));
        variant.setCurrentStock(stock);
        variant.setActive(active);
        return variant;
    }

    @Test
    void listAvailableProducts_onlyIncludesActiveVariants() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant active = variant(true, BigDecimal.TEN);
        ProductVariant inactive = variant(false, BigDecimal.TEN);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(active, inactive));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).variantId()).isEqualTo(active.getId());
    }

    @Test
    void listAvailableProducts_zeroStockVariant_markedNotInStock() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant outOfStock = variant(true, BigDecimal.ZERO);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(outOfStock));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).inStock()).isFalse();
    }

    @Test
    void listAvailableProducts_zeroStockVariant_withStockCheckDisabled_markedInStockAnyway() {
        StorefrontCatalogService lenientCatalogService = new StorefrontCatalogService(
                productRepository, variantRepository, imageRepository, categoryRepository, imageStorageService,
                new StorefrontProperties(null, false));
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant outOfStock = variant(true, BigDecimal.ZERO);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(outOfStock));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        List<PublicVariantResponse> result = lenientCatalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).inStock()).isTrue();
    }

    @Test
    void listAvailableProducts_includesImageUrlsInDisplayOrder() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant active = variant(true, BigDecimal.TEN);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(active));

        ProductVariantImage image = new ProductVariantImage();
        image.setId(UUID.randomUUID());
        image.setVariantId(active.getId());
        image.setFileName("photo-960.jpg");
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(active.getId())).thenReturn(List.of(image));
        when(imageStorageService.mediaUrl(any()))
                .thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).imageUrls()).hasSize(1);
        assertThat(result.get(0).imageUrls().get(0)).contains("photo-960.jpg");
    }

    @Test
    void listAvailableProducts_noRealPhotosButHasStarterImage_fallsBackToTheStarterImage() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant active = variant(true, BigDecimal.TEN);
        active.setStarterImagePath("default-catalog/rice-category/abc.jpg");
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(active));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(active.getId())).thenReturn(List.of());
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).imageUrls()).containsExactly("/media/images/default-catalog/rice-category/abc.jpg");
        assertThat(result.get(0).sampleImage()).isTrue();
        assertThat(result.get(0).thumbnailUrls()).isEmpty();
    }

    @Test
    void listAvailableProducts_hasBothRealPhotosAndStarterImage_realPhotosWin() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant active = variant(true, BigDecimal.TEN);
        active.setStarterImagePath("default-catalog/rice-category/abc.jpg");
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(active));

        ProductVariantImage image = new ProductVariantImage();
        image.setId(UUID.randomUUID());
        image.setVariantId(active.getId());
        image.setFileName("photo-960.jpg");
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(active.getId())).thenReturn(List.of(image));
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).imageUrls()).hasSize(1);
        assertThat(result.get(0).imageUrls().get(0)).contains("photo-960.jpg");
        assertThat(result.get(0).sampleImage()).isFalse();
        assertThat(result.get(0).thumbnailUrls()).hasSize(1);
        assertThat(result.get(0).thumbnailUrls().get(0)).endsWith("photo-960-480.jpg");
    }

    @Test
    void listAvailableProducts_noImagesAtAll_isNotASampleAndHasNoUrls() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant(true, BigDecimal.TEN)));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).imageUrls()).isEmpty();
        assertThat(result.get(0).thumbnailUrls()).isEmpty();
        assertThat(result.get(0).sampleImage()).isFalse();
    }

    @Test
    void listAvailableProducts_zoomUrlsRunParallelToImagesWithNullWhereThereIsNoZoomCopy() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        ProductVariant active = variant(true, BigDecimal.TEN);
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(active));

        ProductVariantImage withZoom = new ProductVariantImage();
        withZoom.setVariantId(active.getId());
        withZoom.setFileName("a.jpg");
        withZoom.setHasZoom(true);
        ProductVariantImage withoutZoom = new ProductVariantImage();
        withoutZoom.setVariantId(active.getId());
        withoutZoom.setFileName("b.jpg");
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(active.getId()))
                .thenReturn(List.of(withZoom, withoutZoom));
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).zoomUrls()).hasSize(2);
        assertThat(result.get(0).zoomUrls().get(0)).endsWith("a-zoom.jpg");
        assertThat(result.get(0).zoomUrls().get(1)).isNull();
    }

    @Test
    void listAvailableProducts_productHasACategory_includesItsIdAndName() {
        UUID categoryId = UUID.randomUUID();
        Product product = product();
        product.setCategoryId(categoryId);
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Milk & Dairy Products");
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant(true, BigDecimal.TEN)));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(categoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).categoryId()).isEqualTo(categoryId);
        assertThat(result.get(0).categoryName()).isEqualTo("Milk & Dairy Products");
    }

    @Test
    void listAvailableProducts_productHasNoCategory_categoryFieldsAreNull() {
        when(productRepository.findAllByTenantId(tenantId)).thenReturn(List.of(product()));
        when(variantRepository.findAllByProductId(productId)).thenReturn(List.of(variant(true, BigDecimal.TEN)));
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        List<PublicVariantResponse> result = catalogService.listAvailableProducts(tenantId);

        assertThat(result.get(0).categoryId()).isNull();
        assertThat(result.get(0).categoryName()).isNull();
    }
}
