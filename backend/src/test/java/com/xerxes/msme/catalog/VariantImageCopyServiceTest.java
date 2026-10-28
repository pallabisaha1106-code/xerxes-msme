package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.storage.ImageStorageService.StoredImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VariantImageCopyServiceTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductVariantImageRepository imageRepository;
    @Mock private ImageStorageService imageStorageService;

    private VariantImageCopyService service;
    private UUID tenantId;
    private UUID productId;
    private UUID sourceId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        service = new VariantImageCopyService(variantRepository, imageRepository, imageStorageService);
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        sourceId = UUID.randomUUID();
        targetId = UUID.randomUUID();

        lenient().when(variantRepository.findByIdAndTenantId(sourceId, tenantId)).thenReturn(Optional.of(variant(sourceId, productId)));
        lenient().when(variantRepository.findByIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(variant(targetId, productId)));
        lenient().when(imageRepository.save(any(ProductVariantImage.class))).thenAnswer(inv -> {
            ProductVariantImage image = inv.getArgument(0);
            image.setId(UUID.randomUUID());
            return image;
        });
        lenient().when(imageStorageService.mediaUrl(anyString())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
        lenient()
                .when(imageStorageService.downloadWithContentType(anyString()))
                .thenAnswer(inv -> new StoredImage(("bytes of " + inv.getArgument(0)).getBytes(), "image/jpeg"));
    }

    private static ProductVariant variant(UUID id, UUID productId) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProductId(productId);
        return variant;
    }

    private ProductVariantImage sourcePhoto(String fileName, boolean hasZoom) {
        ProductVariantImage image = new ProductVariantImage();
        image.setId(UUID.randomUUID());
        image.setTenantId(tenantId);
        image.setVariantId(sourceId);
        image.setFileName(fileName);
        image.setWidth(1000);
        image.setHeight(1250);
        image.setHasZoom(hasZoom);
        return image;
    }

    @Test
    void copyAll_copiesMainThumbnailAndOriginalIntoTheTargetsFolderAndCreatesARow() {
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceId)).thenReturn(List.of(sourcePhoto("abc.jpg", false)));
        when(imageRepository.countByVariantId(targetId)).thenReturn(0);

        List<ProductVariantImageResponse> copies = service.copyAll(tenantId, productId, sourceId, targetId);

        assertThat(copies).hasSize(1);
        ArgumentCaptor<ProductVariantImage> saved = ArgumentCaptor.forClass(ProductVariantImage.class);
        verify(imageRepository).save(saved.capture());
        ProductVariantImage row = saved.getValue();
        assertThat(row.getVariantId()).isEqualTo(targetId);
        assertThat(row.getTenantId()).isEqualTo(tenantId);
        assertThat(row.getWidth()).isEqualTo(1000);
        assertThat(row.getHeight()).isEqualTo(1250);
        assertThat(row.getFileName()).isNotEqualTo("abc.jpg").endsWith(".jpg");
        assertThat(row.isHasZoom()).isFalse();

        String stem = row.getFileName().replace(".jpg", "");
        String targetFolder = tenantId + "/products/" + targetId + "/";
        verify(imageStorageService).upload(eq(targetFolder + row.getFileName()), any(), eq("image/jpeg"));
        verify(imageStorageService).upload(eq(targetFolder + stem + "-480.jpg"), any(), eq("image/jpeg"));
        verify(imageStorageService).upload(eq(tenantId + "/product-originals/" + targetId + "/" + stem), any(), eq("image/jpeg"));
        verify(imageStorageService, never()).upload(eq(targetFolder + stem + "-zoom.jpg"), any(), anyString());
        assertThat(copies.get(0).imageUrl()).isEqualTo("/media/images/" + targetFolder + row.getFileName());
    }

    @Test
    void copyAll_copiesTheZoomFileWhenTheSourceHasOne() {
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceId)).thenReturn(List.of(sourcePhoto("abc.jpg", true)));
        when(imageRepository.countByVariantId(targetId)).thenReturn(0);

        service.copyAll(tenantId, productId, sourceId, targetId);

        ArgumentCaptor<ProductVariantImage> saved = ArgumentCaptor.forClass(ProductVariantImage.class);
        verify(imageRepository).save(saved.capture());
        assertThat(saved.getValue().isHasZoom()).isTrue();
        String stem = saved.getValue().getFileName().replace(".jpg", "");
        verify(imageStorageService).upload(eq(tenantId + "/products/" + targetId + "/" + stem + "-zoom.jpg"), any(), anyString());
    }

    @Test
    void copyAll_appendsAfterThePhotosTheTargetAlreadyHas() {
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceId))
                .thenReturn(List.of(sourcePhoto("a.jpg", false), sourcePhoto("b.jpg", false)));
        when(imageRepository.countByVariantId(targetId)).thenReturn(2);

        List<ProductVariantImageResponse> copies = service.copyAll(tenantId, productId, sourceId, targetId);

        assertThat(copies).extracting(ProductVariantImageResponse::displayOrder).containsExactly(2, 3);
    }

    @Test
    void copyAll_aMissingOriginalIsFine_photosFromBeforeOriginalsWereKept() {
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceId)).thenReturn(List.of(sourcePhoto("abc.jpg", false)));
        when(imageRepository.countByVariantId(targetId)).thenReturn(0);
        when(imageStorageService.downloadWithContentType(tenantId + "/product-originals/" + sourceId + "/abc")).thenReturn(null);

        assertThat(service.copyAll(tenantId, productId, sourceId, targetId)).hasSize(1);
    }

    @Test
    void copyAll_aMissingMainFile_failsAndRemovesWhatWasAlreadyCopied() {
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceId)).thenReturn(List.of(sourcePhoto("abc.jpg", false)));
        when(imageRepository.countByVariantId(targetId)).thenReturn(0);
        when(imageStorageService.downloadWithContentType(tenantId + "/products/" + sourceId + "/abc-480.jpg")).thenReturn(null);

        assertThrows(BusinessRuleViolationException.class, () -> service.copyAll(tenantId, productId, sourceId, targetId));

        ArgumentCaptor<String> uploaded = ArgumentCaptor.forClass(String.class);
        verify(imageStorageService).upload(uploaded.capture(), any(), anyString());
        verify(imageStorageService).delete(uploaded.getValue());
        verify(imageRepository, never()).save(any());
    }

    @Test
    void copyAll_sourceWithNoPhotos_throwsBusinessRuleViolation() {
        when(imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceId)).thenReturn(List.of());

        assertThrows(BusinessRuleViolationException.class, () -> service.copyAll(tenantId, productId, sourceId, targetId));
    }

    @Test
    void copyAll_sameVariantAsSourceAndTarget_throwsBusinessRuleViolation() {
        assertThrows(BusinessRuleViolationException.class, () -> service.copyAll(tenantId, productId, sourceId, sourceId));
    }

    @Test
    void copyAll_unknownVariant_throwsResourceNotFound() {
        when(variantRepository.findByIdAndTenantId(targetId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.copyAll(tenantId, productId, sourceId, targetId));
    }

    @Test
    void copyAll_variantFromAnotherProduct_throwsResourceNotFound() {
        when(variantRepository.findByIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(variant(targetId, UUID.randomUUID())));

        assertThrows(ResourceNotFoundException.class, () -> service.copyAll(tenantId, productId, sourceId, targetId));
    }
}
