package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceCategory;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import com.xerxes.msme.marketplace.MarketplaceListing;
import com.xerxes.msme.marketplace.MarketplaceListingRepository;
import com.xerxes.msme.storage.ImageStorageService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductVariantImageRepository imageRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private MarketplaceCategoryRepository categoryRepository;

    private ProductImageService productImageService;

    private UUID tenantId;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        productImageService = newService(new ProductImageProperties(720, 900, 0.55, 0.90, 800, 800, 0.5, 2.0));

        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        lenient().when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));

        lenient().when(imageRepository.save(any(ProductVariantImage.class))).thenAnswer(inv -> {
            ProductVariantImage image = inv.getArgument(0);
            if (image.getId() == null) {
                image.setId(UUID.randomUUID());
            }
            return image;
        });
        lenient()
                .when(imageStorageService.mediaUrl(any()))
                .thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
    }

    private ProductImageService newService(ProductImageProperties limits) {
        return new ProductImageService(
                variantRepository, imageRepository, imageStorageService, limits, listingRepository, categoryRepository);
    }

    private void tenantHasCategoryWithProfile(ImageProfile profile) {
        UUID categoryId = UUID.randomUUID();
        MarketplaceListing listing = new MarketplaceListing();
        listing.setCategoryId(categoryId);
        MarketplaceCategory category = new MarketplaceCategory();
        category.setImageProfile(profile);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
    }

    private static byte[] jpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private MockMultipartFile portraitImage() throws IOException {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes(960, 1280));
    }

    @Test
    void upload_validPortraitImage_savesMainAndThumbnailFilesAndReturnsUrls() throws IOException {
        ProductVariantImageResponse response = productImageService.upload(tenantId, productId, variantId, portraitImage());

        verify(imageStorageService, times(3)).upload(any(), any(), eq("image/jpeg"));
        assertThat(response.imageUrl()).contains(tenantId + "/products/" + variantId + "/");
        assertThat(response.thumbnailUrl()).endsWith("-480.jpg");
        assertThat(response.width()).isEqualTo(960);
        assertThat(response.height()).isEqualTo(1280);
        verify(imageRepository).save(any(ProductVariantImage.class));
    }

    @Test
    void upload_largeImage_mainIsCappedAt1600ThumbnailAt480AndDetailShopsAlsoGetAZoomCopy() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", jpegBytes(3000, 4000));

        ProductVariantImageResponse response = productImageService.upload(tenantId, productId, variantId, file);

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        verify(imageStorageService, times(4)).upload(paths.capture(), bytes.capture(), eq("image/jpeg"));
        assertThat(ImageIO.read(new ByteArrayInputStream(bytes.getAllValues().get(0))).getWidth()).isEqualTo(1600);
        assertThat(ImageIO.read(new ByteArrayInputStream(bytes.getAllValues().get(1))).getWidth()).isEqualTo(480);
        assertThat(ImageIO.read(new ByteArrayInputStream(bytes.getAllValues().get(2))).getWidth()).isEqualTo(2400);
        assertThat(paths.getAllValues().get(2)).endsWith("-zoom.jpg");
        ArgumentCaptor<ProductVariantImage> saved = ArgumentCaptor.forClass(ProductVariantImage.class);
        verify(imageRepository).save(saved.capture());
        assertThat(saved.getValue().isHasZoom()).isTrue();
        assertThat(response.imageUrl()).doesNotContain("-zoom");
    }

    @Test
    void upload_imageNoBiggerThanTheMainCopy_getsNoZoomCopy() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "ok.jpg", "image/jpeg", jpegBytes(1500, 2000));

        productImageService.upload(tenantId, productId, variantId, file);

        verify(imageStorageService, times(3)).upload(any(), any(), eq("image/jpeg"));
        ArgumentCaptor<ProductVariantImage> saved = ArgumentCaptor.forClass(ProductVariantImage.class);
        verify(imageRepository).save(saved.capture());
        assertThat(saved.getValue().isHasZoom()).isFalse();
    }

    @Test
    void upload_largeImageInACompactShop_getsNoZoomCopy() throws IOException {
        tenantHasCategoryWithProfile(ImageProfile.COMPACT);
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", jpegBytes(3000, 3000));

        productImageService.upload(tenantId, productId, variantId, file);

        verify(imageStorageService, times(3)).upload(any(), any(), eq("image/jpeg"));
        ArgumentCaptor<ProductVariantImage> saved = ArgumentCaptor.forClass(ProductVariantImage.class);
        verify(imageRepository).save(saved.capture());
        assertThat(saved.getValue().isHasZoom()).isFalse();
    }

    @Test
    void delete_imageWithAZoomCopy_alsoRemovesTheZoomFile() {
        UUID imageId = UUID.randomUUID();
        ProductVariantImage image = new ProductVariantImage();
        image.setId(imageId);
        image.setTenantId(tenantId);
        image.setVariantId(variantId);
        image.setFileName("abc.jpg");
        image.setHasZoom(true);
        when(imageRepository.findByIdAndTenantId(imageId, tenantId)).thenReturn(Optional.of(image));

        productImageService.delete(tenantId, productId, variantId, imageId);

        verify(imageStorageService).delete(tenantId + "/products/" + variantId + "/abc-zoom.jpg");
        verify(imageStorageService).delete(tenantId + "/products/" + variantId + "/abc.jpg");
    }

    @Test
    void upload_imageNarrowerThanMainWidth_isNotUpscaled() throws IOException {
        productImageService.upload(tenantId, productId, variantId, portraitImage());

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(imageStorageService, times(3)).upload(any(), bytes.capture(), eq("image/jpeg"));
        assertThat(ImageIO.read(new ByteArrayInputStream(bytes.getAllValues().get(0))).getWidth()).isEqualTo(960);
    }

    @Test
    void upload_pngWithAlpha_isStoredAsJpegAndTheOriginalKeptAsAPng() throws IOException {
        BufferedImage png = new BufferedImage(800, 1000, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(png, "png", out);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", out.toByteArray());

        productImageService.upload(tenantId, productId, variantId, file);

        verify(imageStorageService, times(2)).upload(any(), any(), eq("image/jpeg"));
        verify(imageStorageService).upload(any(), eq(out.toByteArray()), eq("image/png"));
    }

    @Test
    void upload_keepsTheUntouchedOriginalUnderTheNonPublicOriginalsFolder() throws IOException {
        byte[] bytes = jpegBytes(960, 1280);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", bytes);

        ProductVariantImageResponse response = productImageService.upload(tenantId, productId, variantId, file);

        String stem = response.imageUrl().substring(response.imageUrl().lastIndexOf('/') + 1).replace(".jpg", "");
        verify(imageStorageService)
                .upload(eq(tenantId + "/product-originals/" + variantId + "/" + stem), eq(bytes), eq("image/jpeg"));
    }

    @Test
    void delete_alsoRemovesTheKeptOriginal() {
        UUID imageId = UUID.randomUUID();
        ProductVariantImage image = new ProductVariantImage();
        image.setId(imageId);
        image.setTenantId(tenantId);
        image.setVariantId(variantId);
        image.setFileName("abc.jpg");
        when(imageRepository.findByIdAndTenantId(imageId, tenantId)).thenReturn(Optional.of(image));

        productImageService.delete(tenantId, productId, variantId, imageId);

        verify(imageStorageService).delete(tenantId + "/product-originals/" + variantId + "/abc");
    }

    @Test
    void upload_imageTooSmall_throwsBusinessRuleViolation() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes(400, 533));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productImageService.upload(tenantId, productId, variantId, file));
    }

    @Test
    void upload_limitsComeFromConfig_soAStricterMinimumRejectsAnOtherwiseValidImage() {
        ProductImageService strict = newService(new ProductImageProperties(1000, 1300, 0.55, 0.90, 800, 800, 0.5, 2.0));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> strict.upload(tenantId, productId, variantId, portraitImage()));
    }

    @Test
    void upload_squarePhotoInACompactShop_isAccepted() throws IOException {
        tenantHasCategoryWithProfile(ImageProfile.COMPACT);
        MockMultipartFile square = new MockMultipartFile("file", "pack.jpg", "image/jpeg", jpegBytes(900, 900));

        productImageService.upload(tenantId, productId, variantId, square);

        verify(imageStorageService, times(3)).upload(any(), any(), eq("image/jpeg"));
    }

    @Test
    void upload_squarePhotoInADetailShop_isRejected() throws IOException {
        tenantHasCategoryWithProfile(ImageProfile.DETAIL);
        MockMultipartFile square = new MockMultipartFile("file", "pack.jpg", "image/jpeg", jpegBytes(900, 900));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productImageService.upload(tenantId, productId, variantId, square));
    }

    @Test
    void upload_tooSmallPhotoInACompactShop_isStillRejected() throws IOException {
        tenantHasCategoryWithProfile(ImageProfile.COMPACT);
        MockMultipartFile small = new MockMultipartFile("file", "pack.jpg", "image/jpeg", jpegBytes(500, 500));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productImageService.upload(tenantId, productId, variantId, small));
    }

    @Test
    void rules_reflectTheShopsProfile() {
        tenantHasCategoryWithProfile(ImageProfile.COMPACT);

        var rules = productImageService.rules(tenantId);

        assertThat(rules.profile()).isEqualTo(ImageProfile.COMPACT);
        assertThat(rules.minWidth()).isEqualTo(800);
        assertThat(rules.maxAspectRatio()).isEqualTo(2.0);
    }

    @Test
    void rules_shopWithNoListing_getsTheStricterDetailLimits() {
        var rules = productImageService.rules(tenantId);

        assertThat(rules.profile()).isEqualTo(ImageProfile.DETAIL);
        assertThat(rules.minWidth()).isEqualTo(720);
        assertThat(rules.minHeight()).isEqualTo(900);
    }

    @Test
    void upload_landscapeAspectRatio_throwsBusinessRuleViolation() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes(1200, 800));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productImageService.upload(tenantId, productId, variantId, file));
    }

    @Test
    void upload_unreadableFile_throwsBusinessRuleViolation() {
        MockMultipartFile file = new MockMultipartFile("file", "not-an-image.jpg", "image/jpeg", "garbage".getBytes());

        assertThrows(
                BusinessRuleViolationException.class,
                () -> productImageService.upload(tenantId, productId, variantId, file));
    }

    @Test
    void upload_variantBelongsToDifferentProduct_throwsResourceNotFound() throws IOException {
        UUID otherProductId = UUID.randomUUID();
        MockMultipartFile file = portraitImage();

        assertThrows(
                ResourceNotFoundException.class,
                () -> productImageService.upload(tenantId, otherProductId, variantId, file));
    }

    @Test
    void list_returnsImagesFromRepositoryOrderedByDisplayOrder() {
        ProductVariantImage image = new ProductVariantImage();
        image.setId(UUID.randomUUID());
        image.setVariantId(variantId);
        image.setFileName("abc.jpg");
        image.setWidth(960);
        image.setHeight(1280);
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(variantId)).thenReturn(List.of(image));

        List<ProductVariantImageResponse> result = productImageService.list(tenantId, productId, variantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).imageUrl()).contains("abc.jpg");
        assertThat(result.get(0).thumbnailUrl()).contains("abc-480.jpg");
    }

    @Test
    void delete_removesFilesFromStorageAndDbRow() throws IOException {
        ProductVariantImageResponse uploaded = productImageService.upload(tenantId, productId, variantId, portraitImage());
        ProductVariantImage image = new ProductVariantImage();
        image.setId(uploaded.id());
        image.setTenantId(tenantId);
        image.setVariantId(variantId);
        image.setFileName(uploaded.imageUrl().substring(uploaded.imageUrl().lastIndexOf('/') + 1));
        when(imageRepository.findByIdAndTenantId(uploaded.id(), tenantId)).thenReturn(Optional.of(image));

        productImageService.delete(tenantId, productId, variantId, uploaded.id());

        verify(imageStorageService).delete(tenantId + "/products/" + variantId + "/" + image.getFileName());
        verify(imageStorageService)
                .delete(tenantId + "/products/" + variantId + "/" + image.getFileName().replace(".jpg", "-480.jpg"));
        verify(imageRepository).delete(image);
    }

    @Test
    void delete_unknownImage_throwsResourceNotFound() {
        UUID imageId = UUID.randomUUID();
        when(imageRepository.findByIdAndTenantId(imageId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productImageService.delete(tenantId, productId, variantId, imageId));
    }

    @Test
    void delete_imageBelongsToDifferentVariant_throwsResourceNotFound() {
        UUID imageId = UUID.randomUUID();
        ProductVariantImage image = new ProductVariantImage();
        image.setId(imageId);
        image.setVariantId(UUID.randomUUID());
        when(imageRepository.findByIdAndTenantId(imageId, tenantId)).thenReturn(Optional.of(image));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productImageService.delete(tenantId, productId, variantId, imageId));
    }
}
