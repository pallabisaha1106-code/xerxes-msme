package com.xerxes.msme.sales;

import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.sales.dto.CustomerRequest;
import com.xerxes.msme.sales.dto.CustomerResponse;
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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AuthContext authContext;
    private final BulkImportSupport bulkImportSupport;

    public CustomerController(CustomerService customerService, AuthContext authContext, BulkImportSupport bulkImportSupport) {
        this.customerService = customerService;
        this.authContext = authContext;
        this.bulkImportSupport = bulkImportSupport;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.create(authContext.current().tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public BulkImportResult bulkCreate(@RequestBody List<CustomerRequest> requests) {
        UUID tenantId = authContext.current().tenantId();
        return bulkImportSupport.run(requests, request -> customerService.create(tenantId, request));
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return customerService.list(authContext.current().tenantId());
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return customerService.get(authContext.current().tenantId(), id);
    }
}
