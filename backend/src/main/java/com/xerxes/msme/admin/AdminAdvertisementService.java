package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminAdvertisementResponse;
import com.xerxes.msme.admin.dto.CreateAdvertisementRequest;
import com.xerxes.msme.admin.dto.UpdateAdvertisementRequest;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.Advertisement;
import com.xerxes.msme.marketplace.AdvertisementRepository;
import com.xerxes.msme.marketplace.AdvertisementSlot;
import com.xerxes.msme.storage.ImageStorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Super Admin CRUD + publish/unpublish for the marketplace's 3 ad slots (see
 * Advertisement). Creating one is a two-step flow, same as DefaultProduct:
 * create() first (as an unpublished draft with no image yet), then
 * uploadPhoto() - publish() refuses to go live without an image.
 */
@Service
public class AdminAdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final ImageStorageService imageStorageService;

    public AdminAdvertisementService(AdvertisementRepository advertisementRepository, ImageStorageService imageStorageService) {
        this.advertisementRepository = advertisementRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public List<AdminAdvertisementResponse> listBySlot(AdvertisementSlot slot) {
        return advertisementRepository.findAllBySlotOrderByStartsAtDesc(slot).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminAdvertisementResponse create(CreateAdvertisementRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessRuleViolationException("End date must be after the start date.");
        }
        Advertisement advertisement = new Advertisement();
        advertisement.setSlot(request.slot());
        advertisement.setLinkUrl(request.linkUrl());
        advertisement.setAltText(request.altText());
        advertisement.setHeadline(request.headline());
        advertisement.setDescription(request.description());
        advertisement.setStartsAt(request.startsAt());
        advertisement.setEndsAt(request.endsAt());
        advertisement = advertisementRepository.save(advertisement);
        return toResponse(advertisement);
    }

    @Transactional
    public AdminAdvertisementResponse update(UUID id, UpdateAdvertisementRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessRuleViolationException("End date must be after the start date.");
        }
        Advertisement advertisement = findOrThrow(id);
        advertisement.setLinkUrl(request.linkUrl());
        advertisement.setAltText(request.altText());
        advertisement.setHeadline(request.headline());
        advertisement.setDescription(request.description());
        advertisement.setStartsAt(request.startsAt());
        advertisement.setEndsAt(request.endsAt());
        advertisementRepository.save(advertisement);
        return toResponse(advertisement);
    }

    @Transactional
    public AdminAdvertisementResponse uploadPhoto(UUID id, MultipartFile file) {
        Advertisement advertisement = findOrThrow(id);
        String objectPath = "advertisements/" + advertisement.getId() + "/" + UUID.randomUUID() + extensionOf(file);
        try {
            imageStorageService.upload(objectPath, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded advertisement image", e);
        }
        String previousPath = advertisement.getImagePath();
        advertisement.setImagePath(objectPath);
        advertisementRepository.save(advertisement);
        if (previousPath != null) imageStorageService.delete(previousPath);
        return toResponse(advertisement);
    }

    @Transactional
    public AdminAdvertisementResponse setPublished(UUID id, boolean published) {
        Advertisement advertisement = findOrThrow(id);
        if (published && advertisement.getImagePath() == null) {
            throw new BusinessRuleViolationException("Upload an image before publishing this advertisement.");
        }
        advertisement.setPublished(published);
        advertisementRepository.save(advertisement);
        return toResponse(advertisement);
    }

    @Transactional
    public void delete(UUID id) {
        Advertisement advertisement = findOrThrow(id);
        if (advertisement.getImagePath() != null) imageStorageService.delete(advertisement.getImagePath());
        advertisementRepository.delete(advertisement);
    }

    private Advertisement findOrThrow(UUID id) {
        return advertisementRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Advertisement", id));
    }

    private AdminAdvertisementResponse toResponse(Advertisement advertisement) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean currentlyLive = advertisement.isPublished()
                && !now.isBefore(advertisement.getStartsAt())
                && !now.isAfter(advertisement.getEndsAt());
        return new AdminAdvertisementResponse(
                advertisement.getId(),
                advertisement.getSlot(),
                advertisement.getImagePath() == null ? null : imageStorageService.mediaUrl(advertisement.getImagePath()),
                advertisement.getLinkUrl(),
                advertisement.getAltText(),
                advertisement.getHeadline(),
                advertisement.getDescription(),
                advertisement.isPublished(),
                advertisement.getStartsAt(),
                advertisement.getEndsAt(),
                currentlyLive);
    }

    private static String extensionOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) return "";
        int dot = original.lastIndexOf('.');
        return dot < 0 ? "" : original.substring(dot);
    }
}
