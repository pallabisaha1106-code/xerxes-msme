package com.xerxes.msme.inventory;

import com.xerxes.msme.inventory.dto.AdjustmentRequest;
import com.xerxes.msme.inventory.dto.LowStockResponse;
import com.xerxes.msme.inventory.dto.StockLedgerResponse;
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
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;
    private final AuthContext authContext;

    public StockController(StockService stockService, AuthContext authContext) {
        this.stockService = stockService;
        this.authContext = authContext;
    }

    @GetMapping("/variants/{variantId}/ledger")
    public List<StockLedgerResponse> ledger(@PathVariable UUID variantId) {
        return stockService.getLedger(authContext.current().tenantId(), variantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/variants/{variantId}/adjustments")
    public ResponseEntity<StockLedgerResponse> adjust(
            @PathVariable UUID variantId, @Valid @RequestBody AdjustmentRequest request) {
        var principal = authContext.current();
        StockLedger ledger = stockService.recordAdjustment(
                principal.tenantId(), variantId, request.signedDelta(), request.remarks(), principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(ledger));
    }

    @GetMapping("/low-stock")
    public List<LowStockResponse> lowStock() {
        return stockService.getLowStockVariants(authContext.current().tenantId()).stream()
                .map(variant -> new LowStockResponse(
                        variant.getId(), variant.getSku(), variant.getCurrentStock(), variant.getReorderThreshold()))
                .toList();
    }

    private StockLedgerResponse toResponse(StockLedger ledger) {
        return new StockLedgerResponse(
                ledger.getId(),
                ledger.getVariantId(),
                ledger.getMovementType(),
                ledger.getQuantityChange(),
                ledger.getBalanceAfter(),
                ledger.getReferenceType(),
                ledger.getReferenceId(),
                ledger.getRemarks(),
                ledger.getMovementDate());
    }
}
