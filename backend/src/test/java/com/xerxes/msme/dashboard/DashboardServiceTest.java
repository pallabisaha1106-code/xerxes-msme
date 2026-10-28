package com.xerxes.msme.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.dashboard.dto.DashboardResponse;
import com.xerxes.msme.inventory.StockService;
import com.xerxes.msme.purchase.VendorRepository;
import com.xerxes.msme.sales.Customer;
import com.xerxes.msme.sales.CustomerRepository;
import com.xerxes.msme.sales.Invoice;
import com.xerxes.msme.sales.InvoiceLineItemRepository;
import com.xerxes.msme.sales.InvoiceRepository;
import com.xerxes.msme.sales.InvoiceStatus;
import com.xerxes.msme.sales.VariantSalesAggregate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private StockService stockService;

    private DashboardService dashboardService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                productRepository, variantRepository, customerRepository, vendorRepository, invoiceRepository,
                invoiceLineItemRepository, stockService);
        tenantId = UUID.randomUUID();

        lenient().when(invoiceLineItemRepository.findTopSellingVariants(any(), any())).thenReturn(List.of());
        lenient()
                .when(invoiceRepository.findTop5ByTenantIdOrderByInvoiceDateDescCreatedAtDesc(any()))
                .thenReturn(List.of());
        lenient().when(productRepository.countByTenantId(any())).thenReturn(0L);
        lenient().when(customerRepository.countByTenantId(any())).thenReturn(0L);
        lenient().when(vendorRepository.countByTenantId(any())).thenReturn(0L);
        lenient().when(stockService.getLowStockVariants(any())).thenReturn(List.of());
    }

    @Test
    void getDashboard_aggregatesAllFigures_returnsFullResponse() {
        when(variantRepository.sumInventoryValue(tenantId)).thenReturn(new BigDecimal("50000.00"));
        when(invoiceRepository.sumTotalBetween(eq(tenantId), any(), any())).thenReturn(new BigDecimal("1200.00"));
        when(productRepository.countByTenantId(tenantId)).thenReturn(12L);
        when(customerRepository.countByTenantId(tenantId)).thenReturn(30L);
        when(vendorRepository.countByTenantId(tenantId)).thenReturn(4L);
        when(stockService.getLowStockVariants(tenantId)).thenReturn(List.of(new ProductVariant(), new ProductVariant()));

        DashboardResponse response = dashboardService.getDashboard(tenantId);

        assertThat(response.inventoryValue()).isEqualByComparingTo("50000.00");
        assertThat(response.salesToday()).isEqualByComparingTo("1200.00");
        assertThat(response.salesThisWeek()).isEqualByComparingTo("1200.00");
        assertThat(response.salesThisMonth()).isEqualByComparingTo("1200.00");
        assertThat(response.totalProducts()).isEqualTo(12);
        assertThat(response.totalCustomers()).isEqualTo(30);
        assertThat(response.totalVendors()).isEqualTo(4);
        assertThat(response.lowStockCount()).isEqualTo(2);
    }

    @Test
    void getDashboard_noDataYet_nullSumsDefaultToZero() {
        when(variantRepository.sumInventoryValue(tenantId)).thenReturn(null);
        when(invoiceRepository.sumTotalBetween(eq(tenantId), any(), any())).thenReturn(null);

        DashboardResponse response = dashboardService.getDashboard(tenantId);

        assertThat(response.inventoryValue()).isEqualByComparingTo("0");
        assertThat(response.salesToday()).isEqualByComparingTo("0");
        assertThat(response.salesThisWeek()).isEqualByComparingTo("0");
        assertThat(response.salesThisMonth()).isEqualByComparingTo("0");
    }

    @Test
    void getDashboard_topSellingVariant_resolvesProductNameAndSku() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        VariantSalesAggregate row = mock(VariantSalesAggregate.class);
        when(row.getVariantId()).thenReturn(variantId);
        when(row.getTotalQuantity()).thenReturn(new BigDecimal("15"));
        when(row.getTotalRevenue()).thenReturn(new BigDecimal("2985.00"));
        when(invoiceLineItemRepository.findTopSellingVariants(eq(tenantId), any(LocalDate.class))).thenReturn(List.of(row));

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku("TSHIRT-M");
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));

        Product product = new Product();
        product.setId(productId);
        product.setName("Cotton T-Shirt");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        DashboardResponse response = dashboardService.getDashboard(tenantId);

        assertThat(response.topProducts()).hasSize(1);
        assertThat(response.topProducts().get(0).productName()).isEqualTo("Cotton T-Shirt");
        assertThat(response.topProducts().get(0).sku()).isEqualTo("TSHIRT-M");
        assertThat(response.topProducts().get(0).quantitySold()).isEqualByComparingTo("15");
    }

    @Test
    void getDashboard_topSellingVariantNoLongerExists_fallsBackToUnknown() {
        UUID variantId = UUID.randomUUID();
        VariantSalesAggregate row = mock(VariantSalesAggregate.class);
        when(row.getVariantId()).thenReturn(variantId);
        when(row.getTotalQuantity()).thenReturn(BigDecimal.ONE);
        when(row.getTotalRevenue()).thenReturn(BigDecimal.TEN);
        when(invoiceLineItemRepository.findTopSellingVariants(eq(tenantId), any(LocalDate.class))).thenReturn(List.of(row));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());

        DashboardResponse response = dashboardService.getDashboard(tenantId);

        assertThat(response.topProducts().get(0).productName()).isEqualTo("Unknown");
        assertThat(response.topProducts().get(0).sku()).isEqualTo("-");
    }

    @Test
    void getDashboard_recentInvoice_resolvesCustomerName() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDisplayNumber("INV/2026-27/000001");
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 20));
        invoice.setTotalAmount(new BigDecimal("500.00"));
        invoice.setStatus(InvoiceStatus.FINAL);
        when(invoiceRepository.findTop5ByTenantIdOrderByInvoiceDateDescCreatedAtDesc(tenantId)).thenReturn(List.of(invoice));

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Priya Sharma");
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.of(customer));

        DashboardResponse response = dashboardService.getDashboard(tenantId);

        assertThat(response.recentInvoices()).hasSize(1);
        assertThat(response.recentInvoices().get(0).customerName()).isEqualTo("Priya Sharma");
        assertThat(response.recentInvoices().get(0).invoiceDisplayNumber()).isEqualTo("INV/2026-27/000001");
    }

    @Test
    void getDashboard_recentInvoiceCustomerDeleted_fallsBackToUnknown() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDisplayNumber("INV/2026-27/000002");
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 20));
        invoice.setTotalAmount(BigDecimal.TEN);
        invoice.setStatus(InvoiceStatus.FINAL);
        when(invoiceRepository.findTop5ByTenantIdOrderByInvoiceDateDescCreatedAtDesc(tenantId)).thenReturn(List.of(invoice));
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.empty());

        DashboardResponse response = dashboardService.getDashboard(tenantId);

        assertThat(response.recentInvoices().get(0).customerName()).isEqualTo("Unknown");
    }
}
