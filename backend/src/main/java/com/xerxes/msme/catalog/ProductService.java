package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductRequest;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.catalog.dto.ProductUpdateRequest;
import com.xerxes.msme.catalog.dto.VariantRequest;
import com.xerxes.msme.catalog.dto.VariantResponse;
import com.xerxes.msme.catalog.dto.VariantScanResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.DuplicateResourceException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.inventory.StockLedgerRepository;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final ImageStorageService imageStorageService;
    private final TenantRepository tenantRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            StockLedgerRepository stockLedgerRepository,
            ImageStorageService imageStorageService,
            TenantRepository tenantRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.imageStorageService = imageStorageService;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public ProductResponse createProduct(UUID tenantId, ProductRequest request) {
        enforceProductLimit(tenantId);

        for (VariantRequest variantRequest : request.variants()) {
            if (variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, variantRequest.sku())) {
                throw new DuplicateResourceException("SKU already exists: " + variantRequest.sku());
            }
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setCategoryId(request.categoryId());
        product.setDefaultHsnCode(request.defaultHsnCode());
        product.setDefaultGstRate(request.defaultGstRate());
        product.setDefaultUom(request.defaultUom());
        product.setFabric(request.fabric());
        product.setCareInstructions(request.careInstructions());
        product.setIncludedItems(request.includedItems());
        product = productRepository.save(product);

        List<ProductVariant> variants = new ArrayList<>();
        for (VariantRequest variantRequest : request.variants()) {
            variants.add(variantRepository.save(toVariantEntity(tenantId, product.getId(), variantRequest)));
        }

        return toResponse(product, variants);
    }

    /**
     * The single choke point for the plan's product-count cap - every path that
     * creates a Product (this controller's single/bulk create, and
     * StarterCatalogService seeding picks one-by-one) funnels through
     * createProduct, so checking here covers all of them without duplication.
     * FREE's limit is 0, so this always rejects for a FREE tenant regardless of
     * current count - matches "no catalogue at all," not just a low cap.
     */
    private void enforceProductLimit(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        int limit = tenant.getSubscriptionPlan().productLimit();
        if (productRepository.countByTenantId(tenantId) >= limit) {
            throw new BusinessRuleViolationException(
                    "Your " + tenant.getSubscriptionPlan().name() + " plan allows up to " + limit
                            + " products. Upgrade to add more.");
        }
    }

    @Transactional
    public ProductResponse updateProduct(UUID tenantId, UUID productId, ProductUpdateRequest request) {
        Product product = productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setCategoryId(request.categoryId());
        product.setDefaultHsnCode(request.defaultHsnCode());
        product.setDefaultGstRate(request.defaultGstRate());
        product.setDefaultUom(request.defaultUom());
        product.setFabric(request.fabric());
        product.setCareInstructions(request.careInstructions());
        product.setIncludedItems(request.includedItems());
        product = productRepository.save(product);

        return toResponse(product, variantRepository.findAllByProductId(productId));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID tenantId, UUID productId) {
        Product product = productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        return toResponse(product, variantRepository.findAllByProductId(product.getId()));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(UUID tenantId) {
        return productRepository.findAllByTenantId(tenantId).stream()
                .map(product -> toResponse(product, variantRepository.findAllByProductId(product.getId())))
                .toList();
    }

    @Transactional
    public ProductResponse addVariant(UUID tenantId, UUID productId, VariantRequest request) {
        Product product = productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        if (variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }

        variantRepository.save(toVariantEntity(tenantId, productId, request));
        return toResponse(product, variantRepository.findAllByProductId(productId));
    }

    @Transactional
    public ProductResponse updateVariant(UUID tenantId, UUID productId, UUID variantId, VariantRequest request) {
        Product product = productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        ProductVariant variant = findVariant(tenantId, productId, variantId);

        if (!variant.getSku().equalsIgnoreCase(request.sku())
                && variantRepository.existsByTenantIdAndSkuIgnoreCase(tenantId, request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }

        variant.setSku(request.sku());
        variant.setBarcode(resolveBarcodeForUpdate(tenantId, variant, request.barcode()));
        variant.setAttributes(request.attributes() != null ? request.attributes() : Map.of());
        variant.setPurchasePrice(request.purchasePrice());
        variant.setSellingPrice(request.sellingPrice());
        variant.setHsnCode(request.hsnCode());
        variant.setGstRate(request.gstRate());
        variant.setUom(request.uom());
        variant.setReorderThreshold(request.reorderThreshold() != null ? request.reorderThreshold() : BigDecimal.ZERO);
        variantRepository.save(variant);

        return toResponse(product, variantRepository.findAllByProductId(productId));
    }

    /**
     * Variants referenced by stock_ledger rows can't be hard-deleted (no
     * cascade, by design - see the schema header comment) so those are
     * deactivated instead; a never-transacted variant is removed outright,
     * unless it's the product's only variant.
     */
    @Transactional
    public ProductResponse deleteVariant(UUID tenantId, UUID productId, UUID variantId) {
        Product product = productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        ProductVariant variant = findVariant(tenantId, productId, variantId);

        if (stockLedgerRepository.existsByTenantIdAndVariantId(tenantId, variantId)) {
            variant.setActive(false);
            variantRepository.save(variant);
        } else {
            if (variantRepository.countByProductId(productId) <= 1) {
                throw new BusinessRuleViolationException(
                        "Cannot delete the only variant of a product. Add another variant first.");
            }
            variantRepository.delete(variant);
            imageStorageService.deleteAllUnderPrefix(tenantId + "/products/" + variantId + "/");
            imageStorageService.deleteAllUnderPrefix(ProductImageService.originalsPrefix(tenantId, variantId));
        }

        return toResponse(product, variantRepository.findAllByProductId(productId));
    }

    /**
     * Every variant needs a scannable barcode (see the POS quick-billing
     * flow), so one is auto-generated whenever the request doesn't supply
     * one; a manually-supplied barcode is still validated for uniqueness -
     * the DB has a matching partial unique index as a backstop.
     */
    @Transactional(readOnly = true)
    public VariantScanResponse findVariantByBarcode(UUID tenantId, String barcode) {
        ProductVariant variant = variantRepository
                .findByTenantIdAndBarcodeAndActiveTrue(tenantId, barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No active product found for barcode: " + barcode));
        Product product = productRepository
                .findByIdAndTenantId(variant.getProductId(), tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", variant.getProductId()));

        return new VariantScanResponse(
                product.getId(),
                product.getName(),
                variant.getId(),
                variant.getSku(),
                variant.getBarcode(),
                variant.getAttributes(),
                variant.getSellingPrice(),
                VariantAttributeResolver.resolveGstRate(product, variant),
                VariantAttributeResolver.resolveHsnCode(product, variant),
                variant.getCurrentStock());
    }

    private ProductVariant findVariant(UUID tenantId, UUID productId, UUID variantId) {
        ProductVariant variant = variantRepository
                .findByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", variantId));
        if (!variant.getProductId().equals(productId)) {
            throw ResourceNotFoundException.of("ProductVariant", variantId);
        }
        return variant;
    }

    private String resolveBarcodeForCreate(UUID tenantId, String requestedBarcode) {
        if (requestedBarcode == null || requestedBarcode.isBlank()) {
            return generateUniqueBarcode(tenantId);
        }
        if (variantRepository.existsByTenantIdAndBarcode(tenantId, requestedBarcode)) {
            throw new DuplicateResourceException("Barcode already exists: " + requestedBarcode);
        }
        return requestedBarcode;
    }

    private String resolveBarcodeForUpdate(UUID tenantId, ProductVariant variant, String requestedBarcode) {
        if (requestedBarcode == null || requestedBarcode.isBlank()) {
            return variant.getBarcode() != null ? variant.getBarcode() : generateUniqueBarcode(tenantId);
        }
        if (!requestedBarcode.equals(variant.getBarcode())
                && variantRepository.existsByTenantIdAndBarcode(tenantId, requestedBarcode)) {
            throw new DuplicateResourceException("Barcode already exists: " + requestedBarcode);
        }
        return requestedBarcode;
    }

    /** 12-digit numeric codes scan cleanly under Code128/EAN-13 readers; collisions are astronomically unlikely but checked anyway. */
    private String generateUniqueBarcode(UUID tenantId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L));
            if (!variantRepository.existsByTenantIdAndBarcode(tenantId, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique barcode after multiple attempts");
    }

    private ProductVariant toVariantEntity(UUID tenantId, UUID productId, VariantRequest request) {
        ProductVariant variant = new ProductVariant();
        variant.setTenantId(tenantId);
        variant.setProductId(productId);
        variant.setSku(request.sku());
        variant.setBarcode(resolveBarcodeForCreate(tenantId, request.barcode()));
        variant.setAttributes(request.attributes() != null ? request.attributes() : Map.of());
        variant.setPurchasePrice(request.purchasePrice());
        variant.setSellingPrice(request.sellingPrice());
        variant.setHsnCode(request.hsnCode());
        variant.setGstRate(request.gstRate());
        variant.setUom(request.uom());
        variant.setReorderThreshold(request.reorderThreshold() != null ? request.reorderThreshold() : BigDecimal.ZERO);
        return variant;
    }

    private ProductResponse toResponse(Product product, List<ProductVariant> variants) {
        List<VariantResponse> variantResponses = variants.stream()
                .map(variant -> new VariantResponse(
                        variant.getId(),
                        variant.getSku(),
                        variant.getBarcode(),
                        variant.getAttributes(),
                        variant.getPurchasePrice(),
                        variant.getSellingPrice(),
                        variant.getHsnCode(),
                        variant.getGstRate(),
                        variant.getUom(),
                        VariantAttributeResolver.resolveHsnCode(product, variant),
                        VariantAttributeResolver.resolveGstRate(product, variant),
                        VariantAttributeResolver.resolveUom(product, variant),
                        variant.getCurrentStock(),
                        variant.getReorderThreshold(),
                        variant.isActive()))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getCategoryId(),
                product.getDefaultHsnCode(),
                product.getDefaultGstRate(),
                product.getDefaultUom(),
                product.getFabric(),
                product.getCareInstructions(),
                product.getIncludedItems(),
                variantResponses);
    }
}
