package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.ProductCapacityResponse;
import com.xerxes.msme.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/capacity")
public class ProductCapacityController {

    private final ProductCapacityService productCapacityService;
    private final AuthContext authContext;

    public ProductCapacityController(ProductCapacityService productCapacityService, AuthContext authContext) {
        this.productCapacityService = productCapacityService;
        this.authContext = authContext;
    }

    @GetMapping
    public ProductCapacityResponse capacity() {
        return productCapacityService.capacity(authContext.current().tenantId());
    }
}
