package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.storage.ImageStorageService.StoredImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives one variant its own copy of another variant's photos, so a product sold in several sizes can share one
 * photo without uploading it once per size. The files are copied (not shared) because every variant owns the
 * objects under its own folder: deleting a photo, or a variant, removes only that variant's files.
 *
 * Nothing is re-validated or re-encoded here - the photos already passed {@link ProductImageService#upload} when
 * they were first stored - so this is a byte-for-byte copy plus a new database row per photo.
 */
@Service
public class VariantImageCopyService {

    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository imageRepository;
    private final ImageStorageService imageStorageService;

    public VariantImageCopyService(
            ProductVariantRepository variantRepository,
            ProductVariantImageRepository imageRepository,
            ImageStorageService imageStorageService) {
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.imageStorageService = imageStorageService;
    }

    /** Copies every photo of sourceVariantId onto targetVariantId (both must belong to productId) and returns the new photos. */
    @Transactional
    public List<ProductVariantImageResponse> copyAll(UUID tenantId, UUID productId, UUID sourceVariantId, UUID targetVariantId) {
        if (sourceVariantId.equals(targetVariantId)) {
            throw new BusinessRuleViolationException("Choose a different variant to copy photos from.");
        }
        findVariant(tenantId, productId, sourceVariantId);
        findVariant(tenantId, productId, targetVariantId);

        List<ProductVariantImage> sources = imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(sourceVariantId);
        if (sources.isEmpty()) {
            throw new BusinessRuleViolationException("That variant has no photos to copy.");
        }

        int nextOrder = imageRepository.countByVariantId(targetVariantId);
        List<String> written = new ArrayList<>();
        try {
            List<ProductVariantImageResponse> copies = new ArrayList<>();
            for (ProductVariantImage source : sources) {
                copies.add(copyOne(tenantId, source, targetVariantId, nextOrder++, written));
            }
            return copies;
        } catch (RuntimeException e) {
            // A copy that stops halfway must not leave orphaned files behind; the database rows roll back on their own.
            written.forEach(imageStorageService::delete);
            throw e;
        }
    }

    private ProductVariantImageResponse copyOne(
            UUID tenantId, ProductVariantImage source, UUID targetVariantId, int displayOrder, List<String> written) {
        UUID sourceVariantId = source.getVariantId();
        String stem = UUID.randomUUID().toString();
        String fileName = stem + ".jpg";
        String sourceStem = stem(source.getFileName());

        copyObject(
                objectPath(tenantId, sourceVariantId, source.getFileName()),
                objectPath(tenantId, targetVariantId, fileName),
                true,
                written);
        copyObject(
                objectPath(tenantId, sourceVariantId, ProductImageService.thumbnailFileName(source.getFileName())),
                objectPath(tenantId, targetVariantId, ProductImageService.thumbnailFileName(fileName)),
                true,
                written);
        if (source.isHasZoom()) {
            copyObject(
                    objectPath(tenantId, sourceVariantId, ProductImageService.zoomFileName(source.getFileName())),
                    objectPath(tenantId, targetVariantId, ProductImageService.zoomFileName(fileName)),
                    true,
                    written);
        }
        // Photos uploaded before originals were kept have none, so a missing original is fine.
        copyObject(
                ProductImageService.originalsPrefix(tenantId, sourceVariantId) + sourceStem,
                ProductImageService.originalsPrefix(tenantId, targetVariantId) + stem,
                false,
                written);

        ProductVariantImage copy = new ProductVariantImage();
        copy.setTenantId(tenantId);
        copy.setVariantId(targetVariantId);
        copy.setFileName(fileName);
        copy.setWidth(source.getWidth());
        copy.setHeight(source.getHeight());
        copy.setHasZoom(source.isHasZoom());
        copy.setDisplayOrder(displayOrder);
        copy = imageRepository.save(copy);

        return new ProductVariantImageResponse(
                copy.getId(),
                imageStorageService.mediaUrl(objectPath(tenantId, targetVariantId, fileName)),
                imageStorageService.mediaUrl(
                        objectPath(tenantId, targetVariantId, ProductImageService.thumbnailFileName(fileName))),
                copy.getWidth(),
                copy.getHeight(),
                copy.getDisplayOrder());
    }

    private void copyObject(String from, String to, boolean required, List<String> written) {
        StoredImage stored = imageStorageService.downloadWithContentType(from);
        if (stored == null) {
            if (required) {
                throw new BusinessRuleViolationException("A photo file is missing and could not be copied.");
            }
            return;
        }
        imageStorageService.upload(to, stored.content(), stored.contentType() != null ? stored.contentType() : "image/jpeg");
        written.add(to);
    }

    private void findVariant(UUID tenantId, UUID productId, UUID variantId) {
        ProductVariant variant = variantRepository
                .findByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", variantId));
        if (!variant.getProductId().equals(productId)) {
            throw ResourceNotFoundException.of("ProductVariant", variantId);
        }
    }

    private static String stem(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /** Same object-path convention as ProductImageService (kept private there). */
    private static String objectPath(UUID tenantId, UUID variantId, String fileName) {
        return tenantId + "/products/" + variantId + "/" + fileName;
    }
}
