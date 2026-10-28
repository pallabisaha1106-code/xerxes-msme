package com.xerxes.msme.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.xerxes.msme.admin.dto.AdminMarketplaceCategoryResponse;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.ImageProfile;
import com.xerxes.msme.marketplace.MarketplaceCategory;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMarketplaceCategoryServiceTest {

    @Mock private MarketplaceCategoryRepository categoryRepository;

    private AdminMarketplaceCategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new AdminMarketplaceCategoryService(categoryRepository);
    }

    private MarketplaceCategory category(String name, boolean active) {
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setIcon("♦");
        category.setDisplayOrder(1);
        category.setActive(active);
        return category;
    }

    @Test
    void listAll_returnsEveryCategoryRegardlessOfActiveFlag() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(category("Grocery", true), category("Other", false)));

        List<AdminMarketplaceCategoryResponse> result = categoryService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AdminMarketplaceCategoryResponse::active).containsExactly(true, false);
    }

    @Test
    void listAll_exposesWhetherACategoryIsBookable_andNewCategoriesAreNot() {
        MarketplaceCategory salon = category("Salon & Spa", true);
        salon.setBookable(true);
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(salon, category("Grocery", true)));

        List<AdminMarketplaceCategoryResponse> result = categoryService.listAll();

        assertThat(result).extracting(AdminMarketplaceCategoryResponse::bookable).containsExactly(true, false);
    }

    @Test
    void updateStatus_deactivating_flipsTheActiveFlag() {
        MarketplaceCategory category = category("Grocery", true);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        AdminMarketplaceCategoryResponse response = categoryService.updateStatus(category.getId(), false);

        assertThat(response.active()).isFalse();
        assertThat(category.isActive()).isFalse();
    }

    @Test
    void updateStatus_reactivating_flipsBack() {
        MarketplaceCategory category = category("Grocery", false);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        AdminMarketplaceCategoryResponse response = categoryService.updateStatus(category.getId(), true);

        assertThat(response.active()).isTrue();
    }

    @Test
    void updateStatus_unknownCategory_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateStatus(id, false));
    }

    @Test
    void updateImageProfile_changesTheProfileAndDefaultsToCompact() {
        MarketplaceCategory category = category("Ladies Wear", true);
        assertThat(category.getImageProfile()).isEqualTo(ImageProfile.COMPACT);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        AdminMarketplaceCategoryResponse response = categoryService.updateImageProfile(category.getId(), ImageProfile.DETAIL);

        assertThat(response.imageProfile()).isEqualTo(ImageProfile.DETAIL);
        assertThat(category.getImageProfile()).isEqualTo(ImageProfile.DETAIL);
    }

    @Test
    void updateImageProfile_unknownCategory_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateImageProfile(id, ImageProfile.DETAIL));
    }

    @Test
    void updateBookable_flipsTheFlagBothWays() {
        MarketplaceCategory category = category("Salon & Spa", true);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThat(categoryService.updateBookable(category.getId(), true).bookable()).isTrue();
        assertThat(category.isBookable()).isTrue();

        assertThat(categoryService.updateBookable(category.getId(), false).bookable()).isFalse();
        assertThat(category.isBookable()).isFalse();
    }

    @Test
    void updateBookable_unknownCategory_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateBookable(id, true));
    }
}
