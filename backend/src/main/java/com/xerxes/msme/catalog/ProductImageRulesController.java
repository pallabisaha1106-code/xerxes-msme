package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductImageRulesResponse;
import com.xerxes.msme.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-image-rules")
public class ProductImageRulesController {

    private final ProductImageService productImageService;
    private final AuthContext authContext;

    public ProductImageRulesController(ProductImageService productImageService, AuthContext authContext) {
        this.productImageService = productImageService;
        this.authContext = authContext;
    }

    @GetMapping
    public ProductImageRulesResponse rules() {
        return productImageService.rules(authContext.current().tenantId());
    }
}
