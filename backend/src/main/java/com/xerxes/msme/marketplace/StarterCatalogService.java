package com.xerxes.msme.marketplace;

import com.xerxes.msme.catalog.Category;
import com.xerxes.msme.catalog.CategoryRepository;
import com.xerxes.msme.catalog.ProductService;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.catalog.dto.ProductRequest;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.catalog.dto.VariantRequest;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a vendor's picks from the Super Admin-curated starter catalog (see
 * DefaultProductCategory/DefaultProduct) into real Product + ProductVariant
 * rows in their own tenant catalog - a name only, zero price, zero stock;
 * the vendor fills in price via Catalog and stock via Purchases afterward,
 * exactly like a manually-added product. Each product's DefaultProductCategory
 * (e.g. "Rice") is mirrored as the vendor's own Category of the same name -
 * reused if one already exists (e.g. from an earlier pick in this same batch),
 * created otherwise - so picks land in the tenant's catalog properly
 * categorized instead of attached with no category at all. If the
 * DefaultProduct has an image, the new variant references that same GCS
 * object (ProductVariant.starterImagePath) rather than copying it - it can't
 * go through the real photo-upload pipeline anyway, since default-catalog
 * thumbnails are far smaller than a real product photo's quality bar
 * (ProductImageService.validateGuidelines).
 */
@Service
public class    StarterCatalogService {

    private final DefaultProductRepository defaultProductRepository;
    private final DefaultProductCategoryRepository defaultProductCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductService productService;

    public StarterCatalogService(
            DefaultProductRepository defaultProductRepository,
            DefaultProductCategoryRepository defaultProductCategoryRepository,
            CategoryRepository categoryRepository,
            ProductVariantRepository variantRepository,
            ProductService productService) {
        this.defaultProductRepository = defaultProductRepository;
        this.defaultProductCategoryRepository = defaultProductCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
        this.productService = productService;
    }

    @Transactional
    public void createStarterProducts(UUID tenantId, List<UUID> defaultProductIds) {
        // Caches defaultProductCategoryId -> the tenant's own Category id, so
        // picking several products from the same starter sub-category (e.g.
        // three different rice brands) only creates that Category once.
        Map<UUID, UUID> resolvedCategoryIds = new HashMap<>();

        for (UUID defaultProductId : defaultProductIds) {
            DefaultProduct defaultProduct = defaultProductRepository
                    .findById(defaultProductId)
                    .orElseThrow(() -> ResourceNotFoundException.of("DefaultProduct", defaultProductId));

            UUID categoryId = resolvedCategoryIds.computeIfAbsent(
                    defaultProduct.getDefaultProductCategoryId(),
                    defaultProductCategoryId -> findOrCreateTenantCategory(tenantId, defaultProductCategoryId));

            ProductRequest request = new ProductRequest(
                    defaultProduct.getName(),
                    null,
                    null,
                    categoryId,
                    null,
                    BigDecimal.ZERO,
                    "PCS",
                    null,
                    null,
                    null,
                    List.of(new VariantRequest(
                            generateUniqueSku(tenantId, defaultProduct.getName()),
                            null,
                            Map.of(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            null,
                            null,
                            null,
                            null)));
            ProductResponse created = productService.createProduct(tenantId, request);

            if (defaultProduct.getImagePath() != null) {
                UUID variantId = created.variants().get(0).id();
                variantRepository.findById(variantId).ifPresent(variant -> {
                    variant.setStarterImagePath(defaultProduct.getImagePath());
                    variantRepository.save(variant);
                });
            }
        }
    }

    private UUID findOrCreateTenantCategory(UUID tenantId, UUID defaultProductCategoryId) {
        DefaultProductCategory defaultCategory = defaultProductCategoryRepository
                .findById(defaultProductCategoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("DefaultProductCategory", defaultProductCategoryId));

        return categoryRepository
                .findByTenantIdAndNameIgnoreCase(tenantId, defaultCategory.getName())
                .map(Category::getId)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setTenantId(tenantId);
                    category.setName(defaultCategory.getName());
                    return categoryRepository.save(category).getId();
                });
    }

    private String generateUniqueSku(UUID tenantId, String productName) {
        String base = productName.toUpperCase().replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "ITEM";
        if (base.length() > 30) base = base.substring(0, 30);

        String candidate = base;
        int suffix = 1;
        while (variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, candidate)) {
            candidate = base + "-" + ++suffix;
        }
        return candidate;
    }
}
