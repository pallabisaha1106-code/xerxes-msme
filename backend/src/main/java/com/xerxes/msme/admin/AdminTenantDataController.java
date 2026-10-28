package com.xerxes.msme.admin;

import com.xerxes.msme.catalog.ProductService;
import com.xerxes.msme.catalog.dto.ProductResponse;
import com.xerxes.msme.sales.CustomerService;
import com.xerxes.msme.sales.InvoiceService;
import com.xerxes.msme.sales.dto.CustomerResponse;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.storefront.StorefrontOrderService;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only drill-down into one vendor's actual business data, for platform
 * support/investigation. Deliberately thin: every method just calls the
 * exact same service a tenant's own dashboard calls, with the tenantId
 * taken from the path instead of from the caller's own JWT - these services
 * already take tenantId as an explicit parameter, so no tenant-scoped
 * business logic needed to change for this to be safe.
 */
@RestController
@RequestMapping("/api/admin/tenants/{tenantId}")
public class AdminTenantDataController {

    private final ProductService productService;
    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final StorefrontOrderService storefrontOrderService;

    public AdminTenantDataController(
            ProductService productService,
            InvoiceService invoiceService,
            CustomerService customerService,
            StorefrontOrderService storefrontOrderService) {
        this.productService = productService;
        this.invoiceService = invoiceService;
        this.customerService = customerService;
        this.storefrontOrderService = storefrontOrderService;
    }

    @GetMapping("/products")
    public List<ProductResponse> products(@PathVariable UUID tenantId) {
        return productService.listProducts(tenantId);
    }

    @GetMapping("/invoices")
    public List<InvoiceResponse> invoices(@PathVariable UUID tenantId) {
        return invoiceService.listInvoices(tenantId);
    }

    @GetMapping("/customers")
    public List<CustomerResponse> customers(@PathVariable UUID tenantId) {
        return customerService.list(tenantId);
    }

    @GetMapping("/storefront-orders")
    public List<StorefrontOrderResponse> storefrontOrders(@PathVariable UUID tenantId) {
        return storefrontOrderService.list(tenantId, null);
    }
}
