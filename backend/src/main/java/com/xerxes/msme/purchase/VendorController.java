package com.xerxes.msme.purchase;

import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.purchase.dto.VendorRequest;
import com.xerxes.msme.purchase.dto.VendorResponse;
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
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final AuthContext authContext;
    private final BulkImportSupport bulkImportSupport;

    public VendorController(VendorService vendorService, AuthContext authContext, BulkImportSupport bulkImportSupport) {
        this.vendorService = vendorService;
        this.authContext = authContext;
        this.bulkImportSupport = bulkImportSupport;
    }

    @PostMapping
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorRequest request) {
        VendorResponse response = vendorService.create(authContext.current().tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public BulkImportResult bulkCreate(@RequestBody List<VendorRequest> requests) {
        UUID tenantId = authContext.current().tenantId();
        return bulkImportSupport.run(requests, request -> vendorService.create(tenantId, request));
    }

    @GetMapping
    public List<VendorResponse> list() {
        return vendorService.list(authContext.current().tenantId());
    }

    @GetMapping("/{id}")
    public VendorResponse get(@PathVariable UUID id) {
        return vendorService.get(authContext.current().tenantId(), id);
    }
}
