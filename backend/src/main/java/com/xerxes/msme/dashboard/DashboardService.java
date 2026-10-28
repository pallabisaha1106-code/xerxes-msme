package com.xerxes.msme.dashboard;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.dashboard.dto.DashboardResponse;
import com.xerxes.msme.dashboard.dto.RecentInvoiceResponse;
import com.xerxes.msme.dashboard.dto.TopProductResponse;
import com.xerxes.msme.inventory.StockService;
import com.xerxes.msme.purchase.VendorRepository;
import com.xerxes.msme.sales.Customer;
import com.xerxes.msme.sales.CustomerRepository;
import com.xerxes.msme.sales.Invoice;
import com.xerxes.msme.sales.InvoiceLineItemRepository;
import com.xerxes.msme.sales.InvoiceRepository;
import com.xerxes.msme.sales.VariantSalesAggregate;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One read-only endpoint aggregating figures that would otherwise take
 * several round trips from the frontend - inventory valuation, sales
 * totals for a few rolling windows, entity counts, and two small "what
 * needs attention / what's moving" lists.
 */
@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final StockService stockService;

    public DashboardService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CustomerRepository customerRepository,
            VendorRepository vendorRepository,
            InvoiceRepository invoiceRepository,
            InvoiceLineItemRepository invoiceLineItemRepository,
            StockService stockService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.customerRepository = customerRepository;
        this.vendorRepository = vendorRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<TopProductResponse> topProducts = invoiceLineItemRepository
                .findTopSellingVariants(tenantId, thirtyDaysAgo)
                .stream()
                .map(row -> toTopProduct(tenantId, row))
                .toList();

        List<RecentInvoiceResponse> recentInvoices = invoiceRepository
                .findTop5ByTenantIdOrderByInvoiceDateDescCreatedAtDesc(tenantId)
                .stream()
                .map(invoice -> toRecentInvoice(tenantId, invoice))
                .toList();

        return new DashboardResponse(
                nz(variantRepository.sumInventoryValue(tenantId)),
                nz(invoiceRepository.sumTotalBetween(tenantId, today, today)),
                nz(invoiceRepository.sumTotalBetween(tenantId, weekStart, today)),
                nz(invoiceRepository.sumTotalBetween(tenantId, monthStart, today)),
                productRepository.countByTenantId(tenantId),
                customerRepository.countByTenantId(tenantId),
                vendorRepository.countByTenantId(tenantId),
                stockService.getLowStockVariants(tenantId).size(),
                topProducts,
                recentInvoices);
    }

    private TopProductResponse toTopProduct(UUID tenantId, VariantSalesAggregate row) {
        ProductVariant variant =
                variantRepository.findByIdAndTenantId(row.getVariantId(), tenantId).orElse(null);
        if (variant == null) {
            return new TopProductResponse(row.getVariantId(), "Unknown", "-", row.getTotalQuantity(), row.getTotalRevenue());
        }
        Product product =
                productRepository.findByIdAndTenantId(variant.getProductId(), tenantId).orElse(null);
        return new TopProductResponse(
                variant.getId(),
                product != null ? product.getName() : "Unknown",
                variant.getSku(),
                row.getTotalQuantity(),
                row.getTotalRevenue());
    }

    private RecentInvoiceResponse toRecentInvoice(UUID tenantId, Invoice invoice) {
        Customer customer =
                customerRepository.findByIdAndTenantId(invoice.getCustomerId(), tenantId).orElse(null);
        return new RecentInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceDisplayNumber(),
                customer != null ? customer.getName() : "Unknown",
                invoice.getInvoiceDate(),
                invoice.getTotalAmount(),
                invoice.getStatus());
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
