package com.xerxes.msme.storefront;

import com.xerxes.msme.catalog.Category;
import com.xerxes.msme.catalog.CategoryRepository;
import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductImageService;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantImage;
import com.xerxes.msme.catalog.ProductVariantImageRepository;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.storefront.dto.PublicVariantResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the (future) generated storefront site fetches to render its product listing. Public, read-only. */
@Service
public class StorefrontCatalogService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;
    private final StorefrontProperties storefrontProperties;

    public StorefrontCatalogService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductVariantImageRepository imageRepository,
            CategoryRepository categoryRepository,
            ImageStorageService imageStorageService,
            StorefrontProperties storefrontProperties) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.storefrontProperties = storefrontProperties;
    }

    @Transactional(readOnly = true)
    public List<PublicVariantResponse> listAvailableProducts(UUID tenantId) {
        List<Product> products = productRepository.findAllByTenantId(tenantId);
        Map<UUID, String> categoryNamesById = categoryRepository
                .findAllById(products.stream().map(Product::getCategoryId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return products.stream()
                .flatMap(product -> variantRepository.findAllByProductId(product.getId()).stream()
                        .filter(ProductVariant::isActive)
                        .map(variant -> toResponse(tenantId, product, variant, categoryNamesById.get(product.getCategoryId()))))
                .toList();
    }

    private PublicVariantResponse toResponse(UUID tenantId, Product product, ProductVariant variant, String categoryName) {
        List<ProductVariantImage> photos = imageRepository.findAllByVariantIdOrderByDisplayOrderAsc(variant.getId());
        List<String> realPhotos = photos.stream()
                .map(image -> imageStorageService.mediaUrl(
                        tenantId + "/products/" + variant.getId() + "/" + image.getFileName()))
                .toList();
        List<String> thumbnails = photos.stream()
                .map(image -> imageStorageService.mediaUrl(
                        tenantId + "/products/" + variant.getId() + "/" + ProductImageService.thumbnailFileName(image.getFileName())))
                .toList();
        List<String> zoomUrls = photos.stream()
                .map(image -> image.isHasZoom()
                        ? imageStorageService.mediaUrl(
                                tenantId + "/products/" + variant.getId() + "/" + ProductImageService.zoomFileName(image.getFileName()))
                        : null)
                .toList();
        // A vendor's own uploaded photo always wins; the starter-catalog
        // image (if any) is only a placeholder until they add a real one -
        // see ProductVariant.starterImagePath / StarterCatalogService.
        boolean sampleImage = realPhotos.isEmpty() && variant.getStarterImagePath() != null;
        List<String> imageUrls = !realPhotos.isEmpty()
                ? realPhotos
                : variant.getStarterImagePath() == null
                        ? List.of()
                        : List.of(imageStorageService.mediaUrl(variant.getStarterImagePath()));

        return new PublicVariantResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                variant.getId(),
                variant.getSku(),
                variant.getAttributes(),
                variant.getSellingPrice(),
                imageUrls,
                !storefrontProperties.stockCheckEnabled() || variant.getCurrentStock().signum() > 0,
                thumbnails,
                sampleImage,
                zoomUrls,
                product.getCategoryId(),
                categoryName);
    }
}
