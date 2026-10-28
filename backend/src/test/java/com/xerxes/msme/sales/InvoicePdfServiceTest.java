package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;

    private InvoicePdfService pdfService;

    private UUID tenantId;
    private UUID customerId;
    private UUID invoiceId;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        pdfService = new InvoicePdfService(
                tenantRepository, customerRepository, invoiceRepository, invoiceLineItemRepository, variantRepository,
                productRepository);

        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Xerxes Apparel Co");
        tenant.setGstin("27AAAPX1234C1Z5");
        tenant.setAddressLine1("12 MG Road");
        tenant.setCity("Mumbai");
        tenant.setPincode("400001");
        return tenant;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("Priya Sharma");
        customer.setStateCode("27");
        customer.setCity("Mumbai");
        return customer;
    }

    private Invoice invoice() {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceDisplayNumber("INV/2026-27/000007");
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 20));
        invoice.setPlaceOfSupplyStateCode("27");
        invoice.setSubtotal(new BigDecimal("897.00"));
        invoice.setCgstAmount(new BigDecimal("53.82"));
        invoice.setSgstAmount(new BigDecimal("53.82"));
        invoice.setIgstAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("1004.64"));
        return invoice;
    }

    private InvoiceLineItem lineItem() {
        InvoiceLineItem lineItem = new InvoiceLineItem();
        lineItem.setVariantId(variantId);
        lineItem.setQuantity(new BigDecimal("3"));
        lineItem.setUnitPrice(new BigDecimal("299.00"));
        lineItem.setCgstAmount(new BigDecimal("53.82"));
        lineItem.setSgstAmount(new BigDecimal("53.82"));
        lineItem.setIgstAmount(BigDecimal.ZERO);
        lineItem.setLineTotal(new BigDecimal("1004.64"));
        return lineItem;
    }

    private void stubVariantAndProduct() {
        Product product = new Product();
        product.setId(productId);
        product.setName("Cotton T-Shirt");
        product.setDefaultHsnCode("6109");
        product.setDefaultGstRate(new BigDecimal("12"));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku("TSHIRT-M-BLU");
        variant.setAttributes(Map.of("Size", "M"));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
    }

    @Test
    void buildHtml_includesBusinessCustomerAndInvoiceDetails() {
        stubVariantAndProduct();

        String html = pdfService.buildHtml(tenant(), customer(), invoice(), List.of(lineItem()));

        assertThat(html)
                .contains("Xerxes Apparel Co")
                .contains("Priya Sharma")
                .contains("INV/2026-27/000007")
                .contains("TSHIRT-M-BLU")
                .contains("6109")
                .contains("1004.64")
                .contains("Rupees One Thousand Five Only");
    }

    @Test
    void buildHtml_unknownVariant_throwsResourceNotFound() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> pdfService.buildHtml(tenant(), customer(), invoice(), List.of(lineItem())));
    }

    @Test
    void generateInvoicePdf_producesValidPdfBytes() {
        stubVariantAndProduct();
        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.of(customer()));
        when(invoiceLineItemRepository.findAllByInvoiceId(invoiceId)).thenReturn(List.of(lineItem()));

        InvoicePdfService.InvoicePdf pdf = pdfService.generateInvoicePdf(tenantId, invoiceId);

        assertThat(pdf.filename()).isEqualTo("INV-2026-27-000007.pdf");
        assertThat(pdf.content()).isNotEmpty();
        assertThat(new String(pdf.content(), 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void generateInvoicePdf_unknownInvoice_throwsResourceNotFound() {
        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pdfService.generateInvoicePdf(tenantId, invoiceId));
    }

    @Test
    void generateInvoicePdf_missingTenant_throwsIllegalState() {
        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> pdfService.generateInvoicePdf(tenantId, invoiceId));
    }

    @Test
    void generateInvoicePdf_unknownCustomer_throwsResourceNotFound() {
        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pdfService.generateInvoicePdf(tenantId, invoiceId));
    }
}
