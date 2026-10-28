package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.CategoryRequest;
import com.xerxes.msme.catalog.dto.CategoryResponse;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse create(UUID tenantId, CategoryRequest request) {
        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName(request.name());
        category.setParentCategoryId(request.parentCategoryId());
        category = categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID tenantId) {
        return categoryRepository.findAllByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID tenantId, UUID categoryId) {
        Category category = categoryRepository
                .findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
        return toResponse(category);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getParentCategoryId());
    }
}
