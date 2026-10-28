package com.xerxes.msme.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.dto.MarketplaceListingResponse;
import com.xerxes.msme.marketplace.dto.UpdateMarketplaceListingRequest;
import com.xerxes.msme.storage.ImageStorageService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
class MarketplaceListingServiceTest {

    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private MarketplaceCategoryRepository categoryRepository;
    @Mock private ImageStorageService imageStorageService;

    private MarketplaceListingService listingService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        listingService = new MarketplaceListingService(listingRepository, categoryRepository, imageStorageService);
        tenantId = UUID.randomUUID();

        lenient().when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> {
            MarketplaceListing listing = inv.getArgument(0);
            if (listing.getId() == null) listing.setId(UUID.randomUUID());
            return listing;
        });
        lenient()
                .when(imageStorageService.mediaUrl(any()))
                .thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
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

    private MockMultipartFile landscapePhoto() throws IOException {
        return new MockMultipartFile("file", "shop.jpg", "image/jpeg", jpegBytes(1600, 1000));
    }

    private UpdateMarketplaceListingRequest validRequest(UUID categoryId) {
        return new UpdateMarketplaceListingRequest(
                categoryId, "Gujar Nagar", null, "https://instagram.com/x", null, null, null, null, null,
                "+91 98765 43210", MarketplaceOrderMode.WHATSAPP);
    }

    @Test
    void getListing_noExistingRow_returnsDefaultDraftWithoutPersisting() {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        MarketplaceListingResponse response = listingService.getListing(tenantId);

        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.DRAFT);
        assertThat(response.orderMode()).isEqualTo(MarketplaceOrderMode.WHATSAPP);
        assertThat(response.id()).isNull();
        verify(listingRepository, never()).save(any());
    }

    @Test
    void getListing_existingRow_resolvesCategoryName() {
        UUID categoryId = UUID.randomUUID();
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(UUID.randomUUID());
        listing.setTenantId(tenantId);
        listing.setCategoryId(categoryId);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(categoryId);
        category.setName("Ladies Wear");
        category.setIcon("♧");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        MarketplaceListingResponse response = listingService.getListing(tenantId);

        assertThat(response.categoryName()).isEqualTo("Ladies Wear");
    }

    @Test
    void updateListing_noExistingRow_createsAndPersistsADraft() {
        UUID categoryId = UUID.randomUUID();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        MarketplaceListingResponse response = listingService.updateListing(tenantId, validRequest(categoryId));

        assertThat(response.area()).isEqualTo("Gujar Nagar");
        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.DRAFT);
        verify(listingRepository).save(any(MarketplaceListing.class));
    }

    @Test
    void updateListing_normalizesEnquiryWhatsappToDigitsOnly() {
        UUID categoryId = UUID.randomUUID();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        MarketplaceListingResponse response = listingService.updateListing(tenantId, validRequest(categoryId));

        assertThat(response.enquiryWhatsapp()).isEqualTo("919876543210");
    }

    @Test
    void updateListing_existingRow_updatesInPlaceRatherThanCreatingANewOne() {
        UUID categoryId = UUID.randomUUID();
        MarketplaceListing existing = new MarketplaceListing();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(tenantId);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        listingService.updateListing(tenantId, validRequest(categoryId));

        ArgumentCaptor<MarketplaceListing> captor = ArgumentCaptor.forClass(MarketplaceListing.class);
        verify(listingRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
    }

    @Test
    void updateListing_unknownCategory_throwsResourceNotFound() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> listingService.updateListing(tenantId, validRequest(categoryId)));
        verify(listingRepository, never()).save(any());
    }

    @Test
    void uploadPhoto_validLandscapeImage_savesMainAndThumbnailAndReturnsUrl() throws IOException {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        MarketplaceListingResponse response = listingService.uploadPhoto(tenantId, landscapePhoto());

        verify(imageStorageService, times(2)).upload(any(), any(), eq("image/jpeg"));
        assertThat(response.shopPhotoUrl()).contains(tenantId + "/marketplace/");
    }

    @Test
    void uploadPhoto_imageTooSmall_throwsBusinessRuleViolation() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", jpegBytes(400, 300));

        assertThrows(BusinessRuleViolationException.class, () -> listingService.uploadPhoto(tenantId, file));
    }

    @Test
    void uploadPhoto_portraitAspectRatio_throwsBusinessRuleViolation() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", jpegBytes(960, 1280));

        assertThrows(BusinessRuleViolationException.class, () -> listingService.uploadPhoto(tenantId, file));
    }

    @Test
    void adoptPhotoWithoutGuidelines_portraitAspectRatio_doesNotThrowAndStillSavesFiles() throws IOException {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", jpegBytes(960, 1280));

        MarketplaceListingResponse response = listingService.adoptPhotoWithoutGuidelines(tenantId, file);

        verify(imageStorageService, times(2)).upload(any(), any(), eq("image/jpeg"));
        assertThat(response.shopPhotoUrl()).contains(tenantId + "/marketplace/");
    }

    @Test
    void adoptPhotoWithoutGuidelines_tooSmall_doesNotThrow() throws IOException {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", jpegBytes(400, 300));

        MarketplaceListingResponse response = listingService.adoptPhotoWithoutGuidelines(tenantId, file);

        assertThat(response.shopPhotoUrl()).isNotBlank();
    }

    @Test
    void adoptPhotoWithoutGuidelines_unreadableFile_stillThrowsBusinessRuleViolation() {
        MockMultipartFile file = new MockMultipartFile("file", "shop.jpg", "image/jpeg", "not an image".getBytes());

        assertThrows(BusinessRuleViolationException.class, () -> listingService.adoptPhotoWithoutGuidelines(tenantId, file));
    }

    @Test
    void uploadPhoto_unreadableFile_throwsBusinessRuleViolation() {
        MockMultipartFile file = new MockMultipartFile("file", "not-an-image.jpg", "image/jpeg", "garbage".getBytes());

        assertThrows(BusinessRuleViolationException.class, () -> listingService.uploadPhoto(tenantId, file));
    }

    @Test
    void uploadPhoto_replacingExistingPhoto_deletesOldFilesFirst() throws IOException {
        MarketplaceListing existing = new MarketplaceListing();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(tenantId);
        existing.setShopPhotoFileName("old.jpg");
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));

        listingService.uploadPhoto(tenantId, landscapePhoto());

        verify(imageStorageService).delete(tenantId + "/marketplace/old.jpg");
        verify(imageStorageService).delete(tenantId + "/marketplace/old-480.jpg");
        verify(imageStorageService, times(2)).upload(any(), any(), eq("image/jpeg"));
    }

    @Test
    void deletePhoto_removesFilesAndClearsFields() {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(UUID.randomUUID());
        listing.setTenantId(tenantId);
        listing.setShopPhotoFileName("shop.jpg");
        listing.setShopPhotoAlt("alt text");
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        MarketplaceListingResponse response = listingService.deletePhoto(tenantId);

        assertThat(response.shopPhotoUrl()).isNull();
        verify(imageStorageService).delete(tenantId + "/marketplace/shop.jpg");
        verify(imageStorageService).delete(tenantId + "/marketplace/shop-480.jpg");
    }

    @Test
    void deletePhoto_noListingYet_throwsBusinessRuleViolation() {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleViolationException.class, () -> listingService.deletePhoto(tenantId));
    }

    private MarketplaceListing completeDraft() {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(UUID.randomUUID());
        listing.setTenantId(tenantId);
        listing.setCategoryId(UUID.randomUUID());
        listing.setArea("Gujar Nagar");
        listing.setShopPhotoFileName("shop.jpg");
        listing.setEnquiryWhatsapp("919876543210");
        listing.setOrderMode(MarketplaceOrderMode.WHATSAPP);
        return listing;
    }

    @Test
    void submit_allRequiredFieldsPresent_movesToPendingAndStampsSubmittedAt() {
        MarketplaceListing listing = completeDraft();
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        MarketplaceListingResponse response = listingService.submit(tenantId);

        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.PENDING);
        assertThat(listing.getSubmittedAt()).isNotNull();
    }

    @Test
    void submit_missingCategoryAreaAndPhoto_throwsWithAllMissingFieldsNamed() {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(UUID.randomUUID());
        listing.setTenantId(tenantId);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> listingService.submit(tenantId));
        assertThat(ex.getMessage()).contains("category").contains("area").contains("shop photo").contains("WhatsApp number");
    }

    @Test
    void submit_checkoutMode_doesNotRequireEnquiryWhatsapp() {
        MarketplaceListing listing = completeDraft();
        listing.setOrderMode(MarketplaceOrderMode.CHECKOUT);
        listing.setEnquiryWhatsapp(null);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        MarketplaceListingResponse response = listingService.submit(tenantId);

        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.PENDING);
    }

    @Test
    void submit_alreadyPending_throwsBusinessRuleViolation() {
        MarketplaceListing listing = completeDraft();
        listing.setStatus(MarketplaceListingStatus.PENDING);
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        assertThrows(BusinessRuleViolationException.class, () -> listingService.submit(tenantId));
    }

    @Test
    void submit_noListingYet_throwsBusinessRuleViolation() {
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleViolationException.class, () -> listingService.submit(tenantId));
    }

    @Test
    void submit_clearsAnyPreviousRejectionReason() {
        MarketplaceListing listing = completeDraft();
        listing.setRejectionReason("Please add a clearer shop photo.");
        when(listingRepository.findByTenantId(tenantId)).thenReturn(Optional.of(listing));

        MarketplaceListingResponse response = listingService.submit(tenantId);

        assertThat(response.rejectionReason()).isNull();
    }
}
