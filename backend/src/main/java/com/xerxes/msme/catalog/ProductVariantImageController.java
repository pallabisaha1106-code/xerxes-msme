package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.security.AuthContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products/{productId}/variants/{variantId}/images")
public class ProductVariantImageController {

    private final ProductImageService productImageService;
    private final AuthContext authContext;

    public ProductVariantImageController(ProductImageService productImageService, AuthContext authContext) {
        this.productImageService = productImageService;
        this.authContext = authContext;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductVariantImageResponse> upload(
            @PathVariable UUID productId, @PathVariable UUID variantId, @RequestParam("file") MultipartFile file) {
        ProductVariantImageResponse response =
                productImageService.upload(authContext.current().tenantId(), productId, variantId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ProductVariantImageResponse> list(@PathVariable UUID productId, @PathVariable UUID variantId) {
        return productImageService.list(authContext.current().tenantId(), productId, variantId);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID imageId) {
        productImageService.delete(authContext.current().tenantId(), productId, variantId, imageId);
        return ResponseEntity.noContent().build();
    }
}
