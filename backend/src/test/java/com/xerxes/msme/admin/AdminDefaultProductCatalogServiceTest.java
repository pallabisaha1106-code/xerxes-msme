package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.admin.dto.AdminDefaultProductCategoryResponse;
import com.xerxes.msme.admin.dto.AdminDefaultProductResponse;
import com.xerxes.msme.admin.dto.CreateDefaultProductCategoryRequest;
import com.xerxes.msme.admin.dto.CreateDefaultProductRequest;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogRequest;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogResponse;
import com.xerxes.msme.admin.dto.UpdateDefaultProductCategoryRequest;
import com.xerxes.msme.admin.dto.UpdateDefaultProductRequest;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.DefaultProduct;
import com.xerxes.msme.marketplace.DefaultProductCategory;
import com.xerxes.msme.marketplace.DefaultProductCategoryRepository;
import com.xerxes.msme.marketplace.DefaultProductRepository;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import com.xerxes.msme.storage.ImageStorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminDefaultProductCatalogServiceTest {

    @Mock private DefaultProductCategoryRepository categoryRepository;
    @Mock private DefaultProductRepository productRepository;
    @Mock private MarketplaceCategoryRepository marketplaceCategoryRepository;
    @Mock private ImageStorageService imageStorageService;

    private AdminDefaultProductCatalogService catalogService;
    private UUID marketplaceCategoryId;

    @BeforeEach
    void setUp() {
        catalogService = new AdminDefaultProductCatalogService(
                categoryRepository, productRepository, marketplaceCategoryRepository, imageStorageService, new DefaultCatalogProperties(0));
        marketplaceCategoryId = UUID.randomUUID();
        lenient().when(categoryRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listCategories_returnsAllCategoriesUnderMarketplaceCategory() {
        DefaultProductCategory rice = new DefaultProductCategory();
        rice.setId(UUID.randomUUID());
        rice.setMarketplaceCategoryId(marketplaceCategoryId);
        rice.setName("Rice");
        when(categoryRepository.findAllByMarketplaceCategoryIdOrderByDisplayOrderAsc(marketplaceCategoryId)).thenReturn(List.of(rice));

        List<AdminDefaultProductCategoryResponse> result = catalogService.listCategories(marketplaceCategoryId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Rice");
    }

    @Test
    void createCategory_validMarketplaceCategory_createsAndReturns() {
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(true);
        CreateDefaultProductCategoryRequest request = new CreateDefaultProductCategoryRequest(marketplaceCategoryId, "Rice", 1);

        AdminDefaultProductCategoryResponse response = catalogService.createCategory(request);

        assertThat(response.name()).isEqualTo("Rice");
        assertThat(response.marketplaceCategoryId()).isEqualTo(marketplaceCategoryId);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createCategory_unknownMarketplaceCategory_throwsResourceNotFound() {
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(false);
        CreateDefaultProductCategoryRequest request = new CreateDefaultProductCategoryRequest(marketplaceCategoryId, "Rice", 1);

        assertThrows(ResourceNotFoundException.class, () -> catalogService.createCategory(request));
    }

    @Test
    void updateCategory_existing_updatesFields() {
        DefaultProductCategory rice = new DefaultProductCategory();
        rice.setId(UUID.randomUUID());
        rice.setMarketplaceCategoryId(marketplaceCategoryId);
        rice.setName("Rice");
        when(categoryRepository.findById(rice.getId())).thenReturn(Optional.of(rice));

        AdminDefaultProductCategoryResponse response =
                catalogService.updateCategory(rice.getId(), new UpdateDefaultProductCategoryRequest("Rice & Grains", 2, false));

        assertThat(response.name()).isEqualTo("Rice & Grains");
        assertThat(response.displayOrder()).isEqualTo(2);
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateCategory_unknown_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.updateCategory(id, new UpdateDefaultProductCategoryRequest("Rice", 1, true)));
    }

    @Test
    void listProducts_returnsAllProductsUnderCategory() {
        UUID categoryId = UUID.randomUUID();
        DefaultProduct product = new DefaultProduct();
        product.setId(UUID.randomUUID());
        product.setDefaultProductCategoryId(categoryId);
        product.setName("Kohinoor Basmati Rice");
        when(productRepository.findAllByDefaultProductCategoryIdOrderByDisplayOrderAsc(categoryId)).thenReturn(List.of(product));

        List<AdminDefaultProductResponse> result = catalogService.listProducts(categoryId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Kohinoor Basmati Rice");
    }

    @Test
    void createProduct_validCategory_createsAndReturns() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        CreateDefaultProductRequest request = new CreateDefaultProductRequest(categoryId, "Kohinoor Basmati Rice", 1);

        AdminDefaultProductResponse response = catalogService.createProduct(request);

        assertThat(response.name()).isEqualTo("Kohinoor Basmati Rice");
        assertThat(response.defaultProductCategoryId()).isEqualTo(categoryId);
    }

    @Test
    void createProduct_unknownCategory_throwsResourceNotFound() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.existsById(categoryId)).thenReturn(false);
        CreateDefaultProductRequest request = new CreateDefaultProductRequest(categoryId, "Kohinoor Basmati Rice", 1);

        assertThrows(ResourceNotFoundException.class, () -> catalogService.createProduct(request));
    }

    @Test
    void updateProduct_existing_updatesFields() {
        DefaultProduct product = new DefaultProduct();
        product.setId(UUID.randomUUID());
        product.setName("Kohinoor Basmati Rice");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        AdminDefaultProductResponse response =
                catalogService.updateProduct(product.getId(), new UpdateDefaultProductRequest("India Gate Basmati Rice", 2, false));

        assertThat(response.name()).isEqualTo("India Gate Basmati Rice");
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateProduct_unknown_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> catalogService.updateProduct(id, new UpdateDefaultProductRequest("Name", 1, true)));
    }

    @Test
    void uploadProductPhoto_existingProduct_uploadsAndSetsImagePath() throws Exception {
        DefaultProduct product = new DefaultProduct();
        product.setId(UUID.randomUUID());
        product.setDefaultProductCategoryId(UUID.randomUUID());
        product.setName("Kohinoor Basmati Rice");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(imageStorageService.mediaUrl(ArgumentMatchers.any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "bytes".getBytes());

        AdminDefaultProductResponse response = catalogService.uploadProductPhoto(product.getId(), file);

        assertThat(product.getImagePath()).isNotBlank();
        assertThat(response.imageUrl()).startsWith("/media/images/default-catalog/" + product.getDefaultProductCategoryId());
        verify(imageStorageService).upload(ArgumentMatchers.eq(product.getImagePath()), ArgumentMatchers.any(), ArgumentMatchers.eq("image/jpeg"));
    }

    @Test
    void uploadProductPhoto_unknownProduct_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "bytes".getBytes());

        assertThrows(ResourceNotFoundException.class, () -> catalogService.uploadProductPhoto(id, file));
    }

    private static MockMultipartFile jpeg(String name, int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return new MockMultipartFile(name, name, "image/jpeg", out.toByteArray());
    }

    private AdminDefaultProductCatalogService withMinImageSize(int minSize) {
        return new AdminDefaultProductCatalogService(
                categoryRepository, productRepository, marketplaceCategoryRepository, imageStorageService, new DefaultCatalogProperties(minSize));
    }

    @Test
    void uploadProductPhoto_tooSmallWhenSizeCheckOn_isRejectedAndNothingUploaded() throws Exception {
        DefaultProduct product = new DefaultProduct();
        product.setId(UUID.randomUUID());
        product.setDefaultProductCategoryId(UUID.randomUUID());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> withMinImageSize(800).uploadProductPhoto(product.getId(), jpeg("small.jpg", 400, 500)));

        verify(imageStorageService, never()).upload(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        assertThat(product.getImagePath()).isNull();
    }

    @Test
    void importCatalog_tooSmallImageWhenSizeCheckOn_isSkippedAndReportedButProductStillImported() throws Exception {
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(true);
        when(categoryRepository.findByMarketplaceCategoryIdAndNameIgnoreCase(marketplaceCategoryId, "Rice"))
                .thenReturn(Optional.empty());
        when(productRepository.findByDefaultProductCategoryIdAndNameIgnoreCase(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        MockMultipartFile small = jpeg("small.jpg", 300, 300);
        MockMultipartFile big = jpeg("big.jpg", 1000, 1000);
        ImportDefaultCatalogRequest request = new ImportDefaultCatalogRequest(
                marketplaceCategoryId,
                List.of(new ImportDefaultCatalogRequest.Category(
                        "Rice",
                        List.of(
                                new ImportDefaultCatalogRequest.Product("Small Rice", "small.jpg"),
                                new ImportDefaultCatalogRequest.Product("Big Rice", "big.jpg")))));

        ImportDefaultCatalogResponse response =
                withMinImageSize(800).importCatalog(request, Map.of("small.jpg", small, "big.jpg", big));

        assertThat(response.productsCreated()).isEqualTo(2);
        assertThat(response.imagesAttached()).isEqualTo(1);
        assertThat(response.skippedImages()).hasSize(1);
        assertThat(response.skippedImages().get(0)).contains("small.jpg").contains("300x300");
    }

    @Test
    void importCatalog_newCategoryAndProducts_createsEverythingAndAttachesMatchedImage() throws Exception {
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(true);
        when(categoryRepository.findByMarketplaceCategoryIdAndNameIgnoreCase(marketplaceCategoryId, "Rice"))
                .thenReturn(Optional.empty());
        when(productRepository.findByDefaultProductCategoryIdAndNameIgnoreCase(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        MockMultipartFile image = new MockMultipartFile("groc-1-1.jpg", "groc-1-1.jpg", "image/jpeg", "bytes".getBytes());

        ImportDefaultCatalogRequest request = new ImportDefaultCatalogRequest(
                marketplaceCategoryId,
                List.of(new ImportDefaultCatalogRequest.Category(
                        "Rice",
                        List.of(
                                new ImportDefaultCatalogRequest.Product("Kohinoor Basmati Rice", "groc-1-1.jpg"),
                                new ImportDefaultCatalogRequest.Product("India Gate Basmati Rice", null)))));

        ImportDefaultCatalogResponse response = catalogService.importCatalog(request, Map.of("groc-1-1.jpg", image));

        assertThat(response.categoriesCreated()).isEqualTo(1);
        assertThat(response.categoriesUpdated()).isEqualTo(0);
        assertThat(response.productsCreated()).isEqualTo(2);
        assertThat(response.imagesAttached()).isEqualTo(1);
        verify(imageStorageService).upload(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq("image/jpeg"));
    }

    @Test
    void importCatalog_existingCategoryAndProduct_updatesInsteadOfDuplicating() {
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(true);
        DefaultProductCategory existingCategory = new DefaultProductCategory();
        existingCategory.setId(UUID.randomUUID());
        existingCategory.setMarketplaceCategoryId(marketplaceCategoryId);
        existingCategory.setName("Rice");
        when(categoryRepository.findByMarketplaceCategoryIdAndNameIgnoreCase(marketplaceCategoryId, "Rice"))
                .thenReturn(Optional.of(existingCategory));
        DefaultProduct existingProduct = new DefaultProduct();
        existingProduct.setId(UUID.randomUUID());
        existingProduct.setDefaultProductCategoryId(existingCategory.getId());
        existingProduct.setName("Kohinoor Basmati Rice");
        when(productRepository.findByDefaultProductCategoryIdAndNameIgnoreCase(existingCategory.getId(), "Kohinoor Basmati Rice"))
                .thenReturn(Optional.of(existingProduct));

        ImportDefaultCatalogRequest request = new ImportDefaultCatalogRequest(
                marketplaceCategoryId,
                List.of(new ImportDefaultCatalogRequest.Category(
                        "Rice", List.of(new ImportDefaultCatalogRequest.Product("Kohinoor Basmati Rice", null)))));

        ImportDefaultCatalogResponse response = catalogService.importCatalog(request, Map.of());

        assertThat(response.categoriesCreated()).isEqualTo(0);
        assertThat(response.categoriesUpdated()).isEqualTo(1);
        assertThat(response.productsCreated()).isEqualTo(0);
        assertThat(response.productsUpdated()).isEqualTo(1);
        assertThat(response.imagesAttached()).isEqualTo(0);
    }

    @Test
    void importCatalog_unknownMarketplaceCategory_throwsResourceNotFound() {
        when(marketplaceCategoryRepository.existsById(marketplaceCategoryId)).thenReturn(false);
        ImportDefaultCatalogRequest request = new ImportDefaultCatalogRequest(
                marketplaceCategoryId,
                List.of(new ImportDefaultCatalogRequest.Category(
                        "Rice", List.of(new ImportDefaultCatalogRequest.Product("Kohinoor Basmati Rice", null)))));

        assertThrows(ResourceNotFoundException.class, () -> catalogService.importCatalog(request, Map.of()));
    }
}
