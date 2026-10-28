package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminMarketplaceCategoryResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceCategory;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin enable/disable of the platform-curated marketplace category
 * taxonomy (see MarketplaceCategory). Deactivating one hides it from
 * PublicMarketplaceService's category list/filters immediately - existing
 * listings/default-catalog entries under it are left alone, not cascaded.
 */
@Service
public class AdminMarketplaceCategoryService {

    private final MarketplaceCategoryRepository categoryRepository;

    public AdminMarketplaceCategoryService(MarketplaceCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminMarketplaceCategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminMarketplaceCategoryResponse updateStatus(UUID id, boolean active) {
        MarketplaceCategory category =
                categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("MarketplaceCategory", id));
        category.setActive(active);
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public AdminMarketplaceCategoryResponse updateImageProfile(UUID id, ImageProfile imageProfile) {
        MarketplaceCategory category =
                categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("MarketplaceCategory", id));
        category.setImageProfile(imageProfile);
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public AdminMarketplaceCategoryResponse updateBookable(UUID id, boolean bookable) {
        MarketplaceCategory category =
                categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("MarketplaceCategory", id));
        category.setBookable(bookable);
        categoryRepository.save(category);
        return toResponse(category);
    }

    private AdminMarketplaceCategoryResponse toResponse(MarketplaceCategory category) {
        return new AdminMarketplaceCategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getDisplayOrder(),
                category.isActive(),
                category.getImageProfile(),
                category.isBookable());
    }
}
