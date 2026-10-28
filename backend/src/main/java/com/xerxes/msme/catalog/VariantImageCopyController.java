package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductVariantImageResponse;
import com.xerxes.msme.security.AuthContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/variants/{variantId}/images/copy-from/{sourceVariantId}")
public class VariantImageCopyController {

    private final VariantImageCopyService variantImageCopyService;
    private final AuthContext authContext;

    public VariantImageCopyController(VariantImageCopyService variantImageCopyService, AuthContext authContext) {
        this.variantImageCopyService = variantImageCopyService;
        this.authContext = authContext;
    }

    /** Gives variantId its own copy of every photo on sourceVariantId. */
    @PostMapping
    public ResponseEntity<List<ProductVariantImageResponse>> copy(
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID sourceVariantId) {
        List<ProductVariantImageResponse> copies =
                variantImageCopyService.copyAll(authContext.current().tenantId(), productId, sourceVariantId, variantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(copies);
    }
}
