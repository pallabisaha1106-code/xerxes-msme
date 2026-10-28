package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductImageRulesResponse;
import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceCategory;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import com.xerxes.msme.marketplace.MarketplaceListing;
import com.xerxes.msme.marketplace.MarketplaceListingRepository;
import com.xerxes.msme.storage.ImageStorageService;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded variant photos against the storefront photography
 * guideline (portrait framing, full garment visible, real resolution - see
 * the Veera Collections reference audit this was modelled on), then saves a
 * main image (up to 1600w, never upscaled) and a 480w responsive thumbnail as JPEG. WebP output (used
 * by that reference site) is deliberately skipped for now - writing WebP in
 * pure Java needs a native codec, which isn't worth the dependency yet.
 */
@Service
public class ProductImageService {

    // Wide enough to stay sharp on retina screens and to allow click-to-enlarge; never upscaled past the original.
    private static final int MAIN_WIDTH = 1600;
    // Extra-large copy for close inspection (DETAIL shops only, and only when the original is bigger than MAIN_WIDTH).
    private static final int ZOOM_WIDTH = 2400;
    private static final int THUMBNAIL_WIDTH = 480;
    private static final float JPEG_QUALITY = 0.9f;
    // Where the untouched upload is kept; never served publicly (see MediaImageController) since it can carry EXIF such as GPS location.
    private static final String ORIGINALS_DIR = "product-originals";

    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository imageRepository;
    private final ImageStorageService imageStorageService;
    private final ProductImageProperties limits;
    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceCategoryRepository categoryRepository;

    public ProductImageService(
            ProductVariantRepository variantRepository,
            ProductVariantImageRepository imageRepository,
            ImageStorageService imageStorageService,
            ProductImageProperties limits,
            MarketplaceListingRepository listingRepository,
            MarketplaceCategoryRepository categoryRepository) {
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.imageStorageService = imageStorageService;
        this.limits = limits;
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
    }

    /** The limits this vendor's uploads are held to, by their shop category's image profile. */
    @Transactional(readOnly = true)
    public ProductImageRulesResponse rules(UUID tenantId) {
        return rulesFor(profileFor(tenantId));
    }

    /**
     * A shop with no listing yet, or no category on it, gets the DETAIL limits - the stricter set every
     * vendor was held to before profiles existed.
     */
    private ImageProfile profileFor(UUID tenantId) {
        return listingRepository
                .findByTenantId(tenantId)
                .map(MarketplaceListing::getCategoryId)
                .flatMap(categoryRepository::findById)
                .map(MarketplaceCategory::getImageProfile)
                .orElse(ImageProfile.DETAIL);
    }

    private ProductImageRulesResponse rulesFor(ImageProfile profile) {
        return profile == ImageProfile.COMPACT
                ? new ProductImageRulesResponse(
                        profile,
                        limits.compactMinWidth(),
                        limits.compactMinHeight(),
                        limits.compactMinAspectRatio(),
                        limits.compactMaxAspectRatio())
                : new ProductImageRulesResponse(
                        profile, limits.minWidth(), limits.minHeight(), limits.minAspectRatio(), limits.maxAspectRatio());
    }

    @Transactional
    public ProductVariantImageResponse upload(UUID tenantId, UUID productId, UUID variantId, MultipartFile file) {
        findVariant(tenantId, productId, variantId);

        BufferedImage original = readImage(file);
        ImageProfile profile = profileFor(tenantId);
        validateGuidelines(original, rulesFor(profile));

        String baseName = UUID.randomUUID().toString();
        String fileName = baseName + ".jpg";
        String thumbnailFileName = baseName + "-480.jpg";
        imageStorageService.upload(
                objectPath(tenantId, variantId, fileName), jpegBytes(resizeTo(original, MAIN_WIDTH)), "image/jpeg");
        imageStorageService.upload(
                objectPath(tenantId, variantId, thumbnailFileName),
                jpegBytes(resizeTo(original, THUMBNAIL_WIDTH)),
                "image/jpeg");

        boolean storeZoom = profile == ImageProfile.DETAIL && original.getWidth() > MAIN_WIDTH;
        if (storeZoom) {
            imageStorageService.upload(
                    objectPath(tenantId, variantId, zoomFileName(fileName)),
                    jpegBytes(resizeTo(original, ZOOM_WIDTH)),
                    "image/jpeg");
        }

        // The exact bytes the vendor uploaded, so the smaller copies can be regenerated later. Stored last so a
        // failure earlier leaves nothing extra behind.
        imageStorageService.upload(
                originalObjectPath(tenantId, variantId, baseName),
                readBytes(file),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream");

        ProductVariantImage image = new ProductVariantImage();
        image.setHasZoom(storeZoom);
        image.setTenantId(tenantId);
        image.setVariantId(variantId);
        image.setFileName(baseName + ".jpg");
        image.setWidth(original.getWidth());
        image.setHeight(original.getHeight());
        image.setDisplayOrder(imageRepository.countByVariantId(variantId));
        image = imageRepository.save(image);

        return toResponse(tenantId, image);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantImageResponse> list(UUID tenantId, UUID productId, UUID variantId) {
        findVariant(tenantId, productId, variantId);
        return imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(variantId).stream()
                .map(image -> toResponse(tenantId, image))
                .toList();
    }

    @Transactional
    public void delete(UUID tenantId, UUID productId, UUID variantId, UUID imageId) {
        findVariant(tenantId, productId, variantId);
        ProductVariantImage image = imageRepository
                .findByIdAndTenantId(imageId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductVariantImage", imageId));
        if (!image.getVariantId().equals(variantId)) {
            throw ResourceNotFoundException.of("ProductVariantImage", imageId);
        }

        imageStorageService.delete(objectPath(tenantId, variantId, image.getFileName()));
        imageStorageService.delete(objectPath(tenantId, variantId, thumbnailFileName(image.getFileName())));
        if (image.isHasZoom()) {
            imageStorageService.delete(objectPath(tenantId, variantId, zoomFileName(image.getFileName())));
        }
        // Photos uploaded before originals were kept have none - deleting a missing object is harmless.
        imageStorageService.delete(originalObjectPath(tenantId, variantId, stem(image.getFileName())));
        imageRepository.delete(image);
    }

    private ProductVariant findVariant(UUID tenantId, UUID productId, UUID variantId) {
        ProductVariant variant = variantRepository
                .findByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", variantId));
        if (!variant.getProductId().equals(productId)) {
            throw ResourceNotFoundException.of("ProductVariant", variantId);
        }
        return variant;
    }

    /**
     * ImageIO.read returns null for a format it doesn't recognise, but
     * throws IOException for a file that looks like a supported format
     * (e.g. a valid PNG header) whose data is truncated or corrupted - both
     * are "the upload wasn't decodable," not a server-side I/O fault, so
     * both become the same client-facing validation error.
     */
    private BufferedImage readImage(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new BusinessRuleViolationException("The uploaded file is not a readable image.");
            }
            return image;
        } catch (IOException e) {
            throw new BusinessRuleViolationException("The uploaded file is not a readable image.");
        }
    }

    private void validateGuidelines(BufferedImage image, ProductImageRulesResponse rules) {
        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = (double) width / height;

        if (width < rules.minWidth() || height < rules.minHeight()) {
            throw new BusinessRuleViolationException(
                    "Image is too small (" + width + "x" + height + "px). Minimum is " + rules.minWidth() + "x"
                            + rules.minHeight() + "px.");
        }
        if (aspectRatio < rules.minAspectRatio() || aspectRatio > rules.maxAspectRatio()) {
            throw new BusinessRuleViolationException(
                    rules.profile() == ImageProfile.COMPACT
                            ? "Image is too tall or too wide - use a photo that is no more than twice as tall as it is wide, or the reverse."
                            : "Image must be portrait (roughly 3:4 to 4:5) with the full garment visible, not cropped.");
        }
    }

    /** Scales down to at most targetWidth (never up), halving step by step so big photos don't alias. */
    private static BufferedImage resizeTo(BufferedImage source, int targetWidth) {
        int finalWidth = Math.min(targetWidth, source.getWidth());
        int finalHeight = (int) Math.round(finalWidth * ((double) source.getHeight() / source.getWidth()));

        BufferedImage current = source;
        while (current.getWidth() / 2 >= finalWidth) {
            current = scale(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        // Always finish with a scale so the result is plain RGB (a PNG with alpha can't be written as JPEG).
        return scale(current, finalWidth, finalHeight);
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private static byte[] jpegBytes(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(imageOut);
            writer.write(null, new IIOImage(image, null, null), params);
            imageOut.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not encode uploaded image", e);
        } finally {
            writer.dispose();
        }
    }

    public static String thumbnailFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return fileName.substring(0, dot) + "-480" + fileName.substring(dot);
    }

    public static String zoomFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return fileName.substring(0, dot) + "-zoom" + fileName.substring(dot);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded image", e);
        }
    }

    private static String stem(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /** Originals are keyed by the photo's stem (its file name without extension); the object has no extension of its own. */
    private static String originalObjectPath(UUID tenantId, UUID variantId, String stem) {
        return originalsPrefix(tenantId, variantId) + stem;
    }

    public static String originalsPrefix(UUID tenantId, UUID variantId) {
        return tenantId + "/" + ORIGINALS_DIR + "/" + variantId + "/";
    }

    /** Same object-path convention the old local folder tree used - see ImageStorageService. */
    private static String objectPath(UUID tenantId, UUID variantId, String fileName) {
        return tenantId + "/products/" + variantId + "/" + fileName;
    }

    private ProductVariantImageResponse toResponse(UUID tenantId, ProductVariantImage image) {
        UUID variantId = image.getVariantId();
        return new ProductVariantImageResponse(
                image.getId(),
                imageStorageService.mediaUrl(objectPath(tenantId, variantId, image.getFileName())),
                imageStorageService.mediaUrl(objectPath(tenantId, variantId, thumbnailFileName(image.getFileName()))),
                image.getWidth(),
                image.getHeight(),
                image.getDisplayOrder());
    }
}
