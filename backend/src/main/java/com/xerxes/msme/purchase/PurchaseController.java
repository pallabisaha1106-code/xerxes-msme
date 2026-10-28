package com.xerxes.msme.purchase;

import com.xerxes.msme.purchase.dto.PurchaseRequest;
import com.xerxes.msme.purchase.dto.PurchaseResponse;
import com.xerxes.msme.security.AuthContext;
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

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final AuthContext authContext;

    public PurchaseController(PurchaseService purchaseService, AuthContext authContext) {
        this.purchaseService = purchaseService;
        this.authContext = authContext;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        var principal = authContext.current();
        PurchaseResponse response = purchaseService.createPurchase(principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<PurchaseResponse> list() {
        return purchaseService.listPurchases(authContext.current().tenantId());
    }

    @GetMapping("/{id}")
    public PurchaseResponse get(@PathVariable UUID id) {
        return purchaseService.getPurchase(authContext.current().tenantId(), id);
    }
}
