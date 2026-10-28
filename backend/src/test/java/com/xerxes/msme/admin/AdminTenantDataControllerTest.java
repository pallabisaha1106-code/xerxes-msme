package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.catalog.ProductService;
import com.xerxes.msme.sales.CustomerService;
import com.xerxes.msme.sales.InvoiceService;
import com.xerxes.msme.storefront.StorefrontOrderService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminTenantDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTenantDataControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;
    @MockBean private InvoiceService invoiceService;
    @MockBean private CustomerService customerService;
    @MockBean private StorefrontOrderService storefrontOrderService;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void products_delegatesToProductServiceWithPathTenantId() throws Exception {
        when(productService.listProducts(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/tenants/" + tenantId + "/products")).andExpect(status().isOk());

        verify(productService).listProducts(tenantId);
    }

    @Test
    void invoices_delegatesToInvoiceServiceWithPathTenantId() throws Exception {
        when(invoiceService.listInvoices(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/tenants/" + tenantId + "/invoices")).andExpect(status().isOk());

        verify(invoiceService).listInvoices(tenantId);
    }

    @Test
    void customers_delegatesToCustomerServiceWithPathTenantId() throws Exception {
        when(customerService.list(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/tenants/" + tenantId + "/customers")).andExpect(status().isOk());

        verify(customerService).list(tenantId);
    }

    @Test
    void storefrontOrders_delegatesToStorefrontOrderServiceWithNoStatusFilter() throws Exception {
        when(storefrontOrderService.list(eq(tenantId), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/tenants/" + tenantId + "/storefront-orders")).andExpect(status().isOk());

        verify(storefrontOrderService).list(tenantId, null);
    }
}
