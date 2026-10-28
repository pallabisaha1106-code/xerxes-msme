package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductRequest;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.catalog.dto.ProductUpdateRequest;
import com.xerxes.msme.catalog.dto.VariantRequest;
import com.xerxes.msme.catalog.dto.VariantScanResponse;
import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final AuthContext authContext;
    private final BulkImportSupport bulkImportSupport;

    public ProductController(ProductService productService, AuthContext authContext, BulkImportSupport bulkImportSupport) {
        this.productService = productService;
        this.authContext = authContext;
        this.bulkImportSupport = bulkImportSupport;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(authContext.current().tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public BulkImportResult bulkCreate(@RequestBody List<ProductRequest> requests) {
        UUID tenantId = authContext.current().tenantId();
        return bulkImportSupport.run(requests, request -> productService.createProduct(tenantId, request));
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listProducts(authContext.current().tenantId());
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return productService.getProduct(authContext.current().tenantId(), id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(authContext.current().tenantId(), id, request);
    }

    @GetMapping("/variants/by-barcode/{barcode}")
    public VariantScanResponse scanBarcode(@PathVariable String barcode) {
        return productService.findVariantByBarcode(authContext.current().tenantId(), barcode);
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ProductResponse> addVariant(
            @PathVariable UUID productId, @Valid @RequestBody VariantRequest request) {
        ProductResponse response = productService.addVariant(authContext.current().tenantId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ProductResponse updateVariant(
            @PathVariable UUID productId, @PathVariable UUID variantId, @Valid @RequestBody VariantRequest request) {
        return productService.updateVariant(authContext.current().tenantId(), productId, variantId, request);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ProductResponse deleteVariant(@PathVariable UUID productId, @PathVariable UUID variantId) {
        return productService.deleteVariant(authContext.current().tenantId(), productId, variantId);
    }
}
