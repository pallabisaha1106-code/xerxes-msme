package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminDefaultProductCategoryResponse;
import com.xerxes.msme.admin.dto.AdminDefaultProductResponse;
import com.xerxes.msme.admin.dto.CreateDefaultProductCategoryRequest;
import com.xerxes.msme.admin.dto.CreateDefaultProductRequest;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogRequest;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogResponse;
import com.xerxes.msme.admin.dto.UpdateDefaultProductCategoryRequest;
import com.xerxes.msme.admin.dto.UpdateDefaultProductRequest;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.marketplace.DefaultProduct;
import com.xerxes.msme.marketplace.DefaultProductCategory;
import com.xerxes.msme.marketplace.DefaultProductCategoryRepository;
import com.xerxes.msme.marketplace.DefaultProductRepository;
import com.xerxes.msme.marketplace.MarketplaceCategoryRepository;
import com.xerxes.msme.storage.ImageStorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Super Admin curation of the starter catalog vendors can pick from at self-registration - see StarterCatalogService. */
@Service
public class AdminDefaultProductCatalogService {

    private final DefaultProductCategoryRepository categoryRepository;
    private final DefaultProductRepository productRepository;
    private final MarketplaceCategoryRepository marketplaceCategoryRepository;
    private final ImageStorageService imageStorageService;
    private final DefaultCatalogProperties defaultCatalogProperties;

    public AdminDefaultProductCatalogService(
            DefaultProductCategoryRepository categoryRepository,
            DefaultProductRepository productRepository,
            MarketplaceCategoryRepository marketplaceCategoryRepository,
            ImageStorageService imageStorageService,
            DefaultCatalogProperties defaultCatalogProperties) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.marketplaceCategoryRepository = marketplaceCategoryRepository;
        this.imageStorageService = imageStorageService;
        this.defaultCatalogProperties = defaultCatalogProperties;
    }

    @Transactional(readOnly = true)
    public List<AdminDefaultProductCategoryResponse> listCategories(UUID marketplaceCategoryId) {
        return categoryRepository.findAllByMarketplaceCategoryIdOrderByDisplayOrderAsc(marketplaceCategoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminDefaultProductCategoryResponse createCategory(CreateDefaultProductCategoryRequest request) {
        if (!marketplaceCategoryRepository.existsById(request.marketplaceCategoryId())) {
            throw ResourceNotFoundException.of("MarketplaceCategory", request.marketplaceCategoryId());
        }
        DefaultProductCategory category = new DefaultProductCategory();
        category.setMarketplaceCategoryId(request.marketplaceCategoryId());
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public AdminDefaultProductCategoryResponse updateCategory(UUID id, UpdateDefaultProductCategoryRequest request) {
        DefaultProductCategory category =
                categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("DefaultProductCategory", id));
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(request.active());
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<AdminDefaultProductResponse> listProducts(UUID defaultProductCategoryId) {
        return productRepository.findAllByDefaultProductCategoryIdOrderByDisplayOrderAsc(defaultProductCategoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminDefaultProductResponse createProduct(CreateDefaultProductRequest request) {
        if (!categoryRepository.existsById(request.defaultProductCategoryId())) {
            throw ResourceNotFoundException.of("DefaultProductCategory", request.defaultProductCategoryId());
        }
        DefaultProduct product = new DefaultProduct();
        product.setDefaultProductCategoryId(request.defaultProductCategoryId());
        product.setName(request.name());
        product.setDisplayOrder(request.displayOrder());
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public AdminDefaultProductResponse updateProduct(UUID id, UpdateDefaultProductRequest request) {
        DefaultProduct product =
                productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("DefaultProduct", id));
        product.setName(request.name());
        product.setDisplayOrder(request.displayOrder());
        product.setActive(request.active());
        productRepository.save(product);
        return toResponse(product);
    }

    private AdminDefaultProductCategoryResponse toResponse(DefaultProductCategory category) {
        return new AdminDefaultProductCategoryResponse(
                category.getId(),
                category.getMarketplaceCategoryId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive());
    }

    private AdminDefaultProductResponse toResponse(DefaultProduct product) {
        return new AdminDefaultProductResponse(
                product.getId(),
                product.getDefaultProductCategoryId(),
                product.getName(),
                product.getDisplayOrder(),
                product.isActive(),
                product.getImagePath() == null ? null : imageStorageService.mediaUrl(product.getImagePath()));
    }

    @Transactional
    public AdminDefaultProductResponse uploadProductPhoto(UUID id, MultipartFile file) {
        DefaultProduct product =
                productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("DefaultProduct", id));
        byte[] bytes = readBytes(file, "product photo");
        String problem = imageProblem("This photo", bytes);
        if (problem != null) {
            throw new BusinessRuleViolationException(problem);
        }
        String objectPath = "default-catalog/" + product.getDefaultProductCategoryId() + "/" + UUID.randomUUID() + extensionOf(file);
        imageStorageService.upload(objectPath, bytes, file.getContentType());
        product.setImagePath(objectPath);
        productRepository.save(product);
        return toResponse(product);
    }

    /**
     * Upserts every category/product in the manifest by name (case-insensitive,
     * scoped under marketplaceCategoryId / the resolved category respectively),
     * so re-submitting the same manifest is safe. A product whose imageFileName
     * matches one of the attached files gets that image uploaded and wired up;
     * an unmatched name, or a product with none, is left as-is.
     */
    @Transactional
    public ImportDefaultCatalogResponse importCatalog(ImportDefaultCatalogRequest request, Map<String, MultipartFile> imagesByFileName) {
        if (!marketplaceCategoryRepository.existsById(request.marketplaceCategoryId())) {
            throw ResourceNotFoundException.of("MarketplaceCategory", request.marketplaceCategoryId());
        }

        int categoriesCreated = 0;
        int categoriesUpdated = 0;
        int productsCreated = 0;
        int productsUpdated = 0;
        int imagesAttached = 0;
        List<String> skippedImages = new ArrayList<>();

        int categoryOrder = 0;
        for (ImportDefaultCatalogRequest.Category categoryEntry : request.categories()) {
            categoryOrder++;
            DefaultProductCategory category = categoryRepository
                    .findByMarketplaceCategoryIdAndNameIgnoreCase(request.marketplaceCategoryId(), categoryEntry.name())
                    .orElse(null);
            if (category == null) {
                category = new DefaultProductCategory();
                category.setMarketplaceCategoryId(request.marketplaceCategoryId());
                category.setName(categoryEntry.name());
                category.setDisplayOrder(categoryOrder);
                categoriesCreated++;
            } else {
                categoriesUpdated++;
            }
            categoryRepository.save(category);

            int productOrder = 0;
            for (ImportDefaultCatalogRequest.Product productEntry : categoryEntry.products()) {
                productOrder++;
                DefaultProduct product = productRepository
                        .findByDefaultProductCategoryIdAndNameIgnoreCase(category.getId(), productEntry.name())
                        .orElse(null);
                boolean isNew = product == null;
                if (isNew) {
                    product = new DefaultProduct();
                    product.setDefaultProductCategoryId(category.getId());
                    product.setName(productEntry.name());
                    product.setDisplayOrder(productOrder);
                }

                MultipartFile image = productEntry.imageFileName() == null ? null : imagesByFileName.get(productEntry.imageFileName());
                if (image != null) {
                    byte[] bytes = readBytes(image, productEntry.imageFileName());
                    String problem = imageProblem(productEntry.imageFileName(), bytes);
                    if (problem != null) {
                        skippedImages.add(problem);
                    } else {
                        String objectPath = "default-catalog/" + category.getId() + "/" + productEntry.imageFileName();
                        imageStorageService.upload(objectPath, bytes, image.getContentType());
                        product.setImagePath(objectPath);
                        imagesAttached++;
                    }
                }

                productRepository.save(product);
                if (isNew) productsCreated++;
                else productsUpdated++;
            }
        }

        return new ImportDefaultCatalogResponse(categoriesCreated, categoriesUpdated, productsCreated, productsUpdated, imagesAttached, skippedImages);
    }

    private static byte[] readBytes(MultipartFile file, String label) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded " + label, e);
        }
    }

    /** Why this photo can't be used as a starter image, or null if it's fine (or the size check is off). */
    private String imageProblem(String label, byte[] bytes) {
        int minSize = defaultCatalogProperties.minImageSize();
        if (minSize <= 0) {
            return null;
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            return label + " is not a readable image";
        }
        if (Math.min(image.getWidth(), image.getHeight()) < minSize) {
            return label + " is too small (" + image.getWidth() + "x" + image.getHeight() + "px) - the short side must be at least "
                    + minSize + "px";
        }
        return null;
    }

    private static String extensionOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) return "";
        int dot = original.lastIndexOf('.');
        return dot < 0 ? "" : original.substring(dot);
    }
}
