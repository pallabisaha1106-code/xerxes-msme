package com.xerxes.msme.marketplace;

import com.xerxes.msme.marketplace.dto.CreateStarterProductsRequest;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The authenticated vendor picking from the Super Admin's starter catalog to seed their own Product catalog. */
@RestController
@RequestMapping("/api/marketplace/starter-products")
public class StarterCatalogController {

    private final StarterCatalogService starterCatalogService;
    private final AuthContext authContext;

    public StarterCatalogController(StarterCatalogService starterCatalogService, AuthContext authContext) {
        this.starterCatalogService = starterCatalogService;
        this.authContext = authContext;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreateStarterProductsRequest request) {
        starterCatalogService.createStarterProducts(authContext.current().tenantId(), request.defaultProductIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
