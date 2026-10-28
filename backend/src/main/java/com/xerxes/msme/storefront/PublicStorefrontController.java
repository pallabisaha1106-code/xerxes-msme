package com.xerxes.msme.storefront;

import com.xerxes.msme.storefront.dto.PublicVariantResponse;
import com.xerxes.msme.storefront.dto.StorefrontOrderRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No authentication - this is what the (future) generated storefront site itself calls. Rate-limited; see WebConfig. */
@RestController
@RequestMapping("/api/public/storefront/{tenantId}")
public class PublicStorefrontController {

    private final StorefrontCatalogService catalogService;
    private final StorefrontOrderService orderService;

    public PublicStorefrontController(StorefrontCatalogService catalogService, StorefrontOrderService orderService) {
        this.catalogService = catalogService;
        this.orderService = orderService;
    }

    @GetMapping("/products")
    public List<PublicVariantResponse> products(@PathVariable UUID tenantId) {
        return catalogService.listAvailableProducts(tenantId);
    }

    @PostMapping("/orders")
    public ResponseEntity<StorefrontOrderResponse> submitOrder(
            @PathVariable UUID tenantId, @Valid @RequestBody StorefrontOrderRequest request) {
        StorefrontOrderResponse response = orderService.submit(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
