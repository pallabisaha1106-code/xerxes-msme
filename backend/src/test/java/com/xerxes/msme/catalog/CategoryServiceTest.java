package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.dto.CategoryRequest;
import com.xerxes.msme.catalog.dto.CategoryResponse;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    private CategoryService categoryService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
        tenantId = UUID.randomUUID();
    }

    @Test
    void create_newName_savesAndReturnsResponse() {
        when(categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, "Apparel")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryResponse response = categoryService.create(tenantId, new CategoryRequest("Apparel", null));

        assertThat(response.name()).isEqualTo("Apparel");
        assertThat(response.id()).isNotNull();
    }

    @Test
    void create_duplicateName_throws() {
        when(categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, "Apparel")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> categoryService.create(tenantId, new CategoryRequest("Apparel", null)));
    }

    @Test
    void list_returnsAllCategoriesForTenant() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Beverages");
        when(categoryRepository.findAllByTenantId(tenantId)).thenReturn(List.of(category));

        List<CategoryResponse> responses = categoryService.list(tenantId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Beverages");
    }

    @Test
    void get_unknownId_throwsResourceNotFound() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.get(tenantId, categoryId));
    }

    @Test
    void get_found_returnsResponse() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Electronics");
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.get(tenantId, categoryId);

        assertThat(response.name()).isEqualTo("Electronics");
    }
}
