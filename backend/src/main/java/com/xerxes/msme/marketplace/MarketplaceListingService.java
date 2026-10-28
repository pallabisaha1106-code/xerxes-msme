package com.xerxes.msme.marketplace;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.storage.ImageStorageService;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A vendor's own view of their marketplace listing: create/edit a draft,
 * upload the shop-front photo, and submit for Super Admin review (see
 * com.xerxes.msme.admin for the review/approval side, once built). Nothing
 * here is visible on the public marketplace until a Super Admin approves it
 * - see MarketplaceListingStatus.
 */
@Service
public class MarketplaceListingService {

    // A shop-front cover photo is landscape, unlike the portrait product
    // photo guideline in ProductImageService - 1.3-2.2 covers roughly 4:3
    // through 2:1, matching the card's own 1.62 display aspect ratio with
    // some tolerance either side for object-fit:cover cropping.
    private static final int MAIN_WIDTH = 960;
    private static final int THUMBNAIL_WIDTH = 480;
    private static final double MIN_ASPECT_RATIO = 1.3;
    private static final double MAX_ASPECT_RATIO = 2.2;
    private static final int MIN_WIDTH = 960;
    private static final int MIN_HEIGHT = 480;

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceCategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    public MarketplaceListingService(
            MarketplaceListingRepository listingRepository,
            MarketplaceCategoryRepository categoryRepository,
            ImageStorageService imageStorageService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public MarketplaceListingResponse getListing(UUID tenantId) {
        return toResponse(findOrNewListing(tenantId));
    }

    @Transactional
    public MarketplaceListingResponse updateListing(UUID tenantId, UpdateMarketplaceListingRequest request) {
        if (request.categoryId() != null && !categoryRepository.existsById(request.categoryId())) {
            throw ResourceNotFoundException.of("MarketplaceCategory", request.categoryId());
        }
        MarketplaceListing listing = findOrNewListing(tenantId);
        listing.setCategoryId(request.categoryId());
        listing.setArea(request.area());
        listing.setShopPhotoAlt(request.shopPhotoAlt());
        listing.setInstagramUrl(request.instagramUrl());
        listing.setFacebookUrl(request.facebookUrl());
        listing.setYoutubeUrl(request.youtubeUrl());
        listing.setGoogleMapsUrl(request.googleMapsUrl());
        listing.setWhatsappUrl(request.whatsappUrl());
        listing.setWebsiteUrl(request.websiteUrl());
        listing.setEnquiryWhatsapp(normalizeDigitsOrNull(request.enquiryWhatsapp()));
        listing.setOrderMode(request.orderMode());
        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional
    public MarketplaceListingResponse uploadPhoto(UUID tenantId, MultipartFile file) {
        BufferedImage original = readImage(file);
        validateGuidelines(original);
        return storePhoto(tenantId, original);
    }

    /**
     * Seeds the listing photo directly from the Step 1 KYC shop-front photo
     * (see VendorApplicationService.apply()). That photo is a review
     * document with no landscape/size requirement - unlike a photo uploaded
     * here via uploadPhoto() - so this intentionally skips validateGuidelines()
     * rather than rejecting a perfectly fine KYC photo or blocking
     * registration over it.
     */
    @Transactional
    public MarketplaceListingResponse adoptPhotoWithoutGuidelines(UUID tenantId, MultipartFile file) {
        return storePhoto(tenantId, readImage(file));
    }

    private MarketplaceListingResponse storePhoto(UUID tenantId, BufferedImage original) {
        MarketplaceListing listing = findOrNewListing(tenantId);
        deleteExistingPhotoFiles(tenantId, listing.getShopPhotoFileName());

        String baseName = UUID.randomUUID().toString();
        imageStorageService.upload(
                objectPath(tenantId, baseName + ".jpg"), jpegBytes(resizeTo(original, MAIN_WIDTH)), "image/jpeg");
        imageStorageService.upload(
                objectPath(tenantId, baseName + "-480.jpg"),
                jpegBytes(resizeTo(original, THUMBNAIL_WIDTH)),
                "image/jpeg");

        listing.setShopPhotoFileName(baseName + ".jpg");
        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional
    public MarketplaceListingResponse deletePhoto(UUID tenantId) {
        MarketplaceListing listing = listingRepository
                .findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleViolationException("No marketplace listing exists yet."));
        deleteExistingPhotoFiles(tenantId, listing.getShopPhotoFileName());
        listing.setShopPhotoFileName(null);
        listing.setShopPhotoAlt(null);
        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional
    public MarketplaceListingResponse submit(UUID tenantId) {
        MarketplaceListing listing = listingRepository
                .findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Complete your marketplace listing before submitting it for review."));
        if (listing.getStatus() != MarketplaceListingStatus.DRAFT) {
            throw new BusinessRuleViolationException("This listing has already been submitted.");
        }

        List<String> missing = new ArrayList<>();
        if (listing.getCategoryId() == null) missing.add("category");
        if (isBlank(listing.getArea())) missing.add("area");
        if (listing.getShopPhotoFileName() == null) missing.add("shop photo");
        if (listing.getOrderMode() == MarketplaceOrderMode.WHATSAPP && isBlank(listing.getEnquiryWhatsapp())) {
            missing.add("WhatsApp number");
        }
        if (!missing.isEmpty()) {
            throw new BusinessRuleViolationException("Before submitting, please add: " + String.join(", ", missing) + ".");
        }

        listing.setStatus(MarketplaceListingStatus.PENDING);
        listing.setSubmittedAt(OffsetDateTime.now());
        listing.setRejectionReason(null);
        listingRepository.save(listing);
        return toResponse(listing);
    }

    private MarketplaceListing findOrNewListing(UUID tenantId) {
        return listingRepository.findByTenantId(tenantId).orElseGet(() -> {
            MarketplaceListing draft = new MarketplaceListing();
            draft.setTenantId(tenantId);
            return draft;
        });
    }

    private void deleteExistingPhotoFiles(UUID tenantId, String fileName) {
        if (fileName == null) return;
        imageStorageService.delete(objectPath(tenantId, fileName));
        imageStorageService.delete(objectPath(tenantId, thumbnailFileName(fileName)));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeDigitsOrNull(String value) {
        if (value == null) return null;
        String digitsOnly = value.replaceAll("[^0-9]", "");
        return digitsOnly.isBlank() ? null : digitsOnly;
    }

    /** Same rationale as ProductImageService.readImage: a null OR a decode failure both mean "not a readable image." */
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

    private void validateGuidelines(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = (double) width / height;

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            throw new BusinessRuleViolationException(
                    "Image is too small (" + width + "x" + height + "px). Minimum is " + MIN_WIDTH + "x" + MIN_HEIGHT + "px.");
        }
        if (aspectRatio < MIN_ASPECT_RATIO || aspectRatio > MAX_ASPECT_RATIO) {
            throw new BusinessRuleViolationException(
                    "Shop photo must be landscape (roughly 4:3 to 2:1), like a storefront photo, not a portrait or square crop.");
        }
    }

    private static BufferedImage resizeTo(BufferedImage source, int targetWidth) {
        int targetHeight = (int) Math.round(targetWidth * ((double) source.getHeight() / source.getWidth()));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    private static byte[] jpegBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not encode uploaded shop photo", e);
        }
    }

    private static String thumbnailFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return fileName.substring(0, dot) + "-480" + fileName.substring(dot);
    }

    /** Same object-path convention the old local folder tree used - see ImageStorageService. */
    private static String objectPath(UUID tenantId, String fileName) {
        return tenantId + "/marketplace/" + fileName;
    }

    private MarketplaceListingResponse toResponse(MarketplaceListing listing) {
        String categoryName = listing.getCategoryId() != null
                ? categoryRepository.findById(listing.getCategoryId()).map(MarketplaceCategory::getName).orElse(null)
                : null;
        String shopPhotoUrl = listing.getShopPhotoFileName() != null
                ? imageStorageService.mediaUrl(objectPath(listing.getTenantId(), listing.getShopPhotoFileName()))
                : null;
        return new MarketplaceListingResponse(
                listing.getId(),
                listing.getCategoryId(),
                categoryName,
                listing.getArea(),
                shopPhotoUrl,
                listing.getShopPhotoAlt(),
                listing.getInstagramUrl(),
                listing.getFacebookUrl(),
                listing.getYoutubeUrl(),
                listing.getGoogleMapsUrl(),
                listing.getWhatsappUrl(),
                listing.getWebsiteUrl(),
                listing.getEnquiryWhatsapp(),
                listing.getOrderMode(),
                listing.getStatus(),
                listing.getRejectionReason(),
                listing.getSubmittedAt());
    }
}
