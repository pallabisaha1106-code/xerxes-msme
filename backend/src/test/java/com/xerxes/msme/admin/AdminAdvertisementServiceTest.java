package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.admin.dto.AdminAdvertisementResponse;
import com.xerxes.msme.admin.dto.CreateAdvertisementRequest;
import com.xerxes.msme.admin.dto.UpdateAdvertisementRequest;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.Advertisement;
import com.xerxes.msme.marketplace.AdvertisementRepository;
import com.xerxes.msme.marketplace.AdvertisementSlot;
import com.xerxes.msme.storage.ImageStorageService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminAdvertisementServiceTest {

    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private ImageStorageService imageStorageService;

    private AdminAdvertisementService advertisementService;

    @BeforeEach
    void setUp() {
        advertisementService = new AdminAdvertisementService(advertisementRepository, imageStorageService);
        // lenient: not every test below exercises a save() call (some throw
        // before reaching it, others only delete/list).
        lenient().when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(inv -> {
            Advertisement a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
    }

    private CreateAdvertisementRequest createRequest() {
        return new CreateAdvertisementRequest(
                AdvertisementSlot.LEFT_RAIL,
                "https://example.com",
                "Example ad",
                "Big sale",
                "50% off everything",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(7));
    }

    @Test
    void create_savesAnUnpublishedDraftWithNoImage() {
        AdminAdvertisementResponse response = advertisementService.create(createRequest());

        assertThat(response.published()).isFalse();
        assertThat(response.currentlyLive()).isFalse();
        assertThat(response.imageUrl()).isNull();
        assertThat(response.slot()).isEqualTo(AdvertisementSlot.LEFT_RAIL);
    }

    @Test
    void create_endDateNotAfterStartDate_throwsBusinessRuleViolation() {
        OffsetDateTime now = OffsetDateTime.now();
        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                AdvertisementSlot.LEFT_RAIL, null, "alt", null, null, now, now);

        assertThrows(BusinessRuleViolationException.class, () -> advertisementService.create(request));
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void uploadPhoto_setsImagePathAndDeletesThePreviousOneIfAny() throws Exception {
        Advertisement advertisement = new Advertisement();
        advertisement.setId(UUID.randomUUID());
        advertisement.setImagePath("advertisements/" + advertisement.getId() + "/old.jpg");
        when(advertisementRepository.findById(advertisement.getId())).thenReturn(Optional.of(advertisement));
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", "bytes".getBytes());

        AdminAdvertisementResponse response = advertisementService.uploadPhoto(advertisement.getId(), file);

        assertThat(advertisement.getImagePath()).isNotEqualTo("advertisements/" + advertisement.getId() + "/old.jpg");
        assertThat(response.imageUrl()).startsWith("/media/images/advertisements/" + advertisement.getId());
        verify(imageStorageService).upload(eq(advertisement.getImagePath()), any(), eq("image/jpeg"));
        verify(imageStorageService).delete("advertisements/" + advertisement.getId() + "/old.jpg");
    }

    @Test
    void uploadPhoto_unknownAdvertisement_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(advertisementRepository.findById(id)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", "bytes".getBytes());

        assertThrows(ResourceNotFoundException.class, () -> advertisementService.uploadPhoto(id, file));
    }

    @Test
    void setPublished_withoutAnImage_throwsBusinessRuleViolation() {
        Advertisement advertisement = new Advertisement();
        advertisement.setId(UUID.randomUUID());
        when(advertisementRepository.findById(advertisement.getId())).thenReturn(Optional.of(advertisement));

        assertThrows(BusinessRuleViolationException.class, () -> advertisementService.setPublished(advertisement.getId(), true));
    }

    @Test
    void setPublished_withAnImage_publishesSuccessfully() {
        Advertisement advertisement = new Advertisement();
        advertisement.setId(UUID.randomUUID());
        advertisement.setImagePath("advertisements/" + advertisement.getId() + "/banner.jpg");
        advertisement.setStartsAt(OffsetDateTime.now().minusDays(1));
        advertisement.setEndsAt(OffsetDateTime.now().plusDays(6));
        when(advertisementRepository.findById(advertisement.getId())).thenReturn(Optional.of(advertisement));
        when(imageStorageService.mediaUrl(any())).thenAnswer(inv -> "/media/images/" + inv.getArgument(0));

        AdminAdvertisementResponse response = advertisementService.setPublished(advertisement.getId(), true);

        assertThat(response.published()).isTrue();
        assertThat(response.currentlyLive()).isTrue();
    }

    @Test
    void update_endDateNotAfterStartDate_throwsBusinessRuleViolationBeforeEvenLookingUpTheAdvertisement() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(null, "alt", null, null, now, now.minusDays(1));

        assertThrows(BusinessRuleViolationException.class, () -> advertisementService.update(id, request));
        verify(advertisementRepository, never()).findById(any());
    }

    @Test
    void delete_removesTheImageFirstIfOneExists() {
        Advertisement advertisement = new Advertisement();
        advertisement.setId(UUID.randomUUID());
        advertisement.setImagePath("advertisements/" + advertisement.getId() + "/banner.jpg");
        when(advertisementRepository.findById(advertisement.getId())).thenReturn(Optional.of(advertisement));

        advertisementService.delete(advertisement.getId());

        verify(imageStorageService).delete(advertisement.getImagePath());
        verify(advertisementRepository).delete(advertisement);
    }

    @Test
    void listBySlot_delegatesToRepository() {
        Advertisement advertisement = new Advertisement();
        advertisement.setId(UUID.randomUUID());
        advertisement.setSlot(AdvertisementSlot.RIGHT_RAIL);
        when(advertisementRepository.findAllBySlotOrderByStartsAtDesc(AdvertisementSlot.RIGHT_RAIL))
                .thenReturn(List.of(advertisement));

        List<AdminAdvertisementResponse> result = advertisementService.listBySlot(AdvertisementSlot.RIGHT_RAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slot()).isEqualTo(AdvertisementSlot.RIGHT_RAIL);
    }
}
