package com.xerxes.msme.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.inventory.StockService;
import com.xerxes.msme.sales.dto.InvoiceLineItemRequest;
import com.xerxes.msme.sales.dto.InvoiceRequest;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
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
class InvoiceServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private InvoiceNumberService invoiceNumberService;
    @Mock private StockService stockService;

    private InvoiceService invoiceService;

    private UUID tenantId;
    private UUID userId;
    private UUID customerId;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                tenantRepository, customerRepository, variantRepository, productRepository, invoiceRepository,
                invoiceLineItemRepository, invoiceNumberService, stockService);

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStateCode("27");
        lenient().when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        Product product = new Product();
        product.setId(productId);
        product.setDefaultGstRate(new BigDecimal("12"));
        lenient().when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        lenient().when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));

        lenient().when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        lenient()
                .when(invoiceLineItemRepository.save(any(InvoiceLineItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient()
                .when(invoiceNumberService.allocateNext(tenantId, LocalDate.of(2026, 6, 20)))
                .thenReturn(new InvoiceNumberAllocation("2026-27", 1L, "INV/2026-27/000001"));
    }

    private Customer customerWithState(String stateCode) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setStateCode(stateCode);
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.of(customer));
        return customer;
    }

    @Test
    void createInvoice_sameStateAsTenant_appliesCgstAndSgstOnly() {
        customerWithState("27");
        InvoiceRequest request = new InvoiceRequest(
                customerId, LocalDate.of(2026, 6, 20), null,
                List.of(new InvoiceLineItemRequest(variantId, new BigDecimal("3"), new BigDecimal("299.00"))));

        InvoiceResponse response = invoiceService.createInvoice(tenantId, userId, request);

        assertThat(response.subtotal()).isEqualByComparingTo("897.00");
        assertThat(response.cgstAmount()).isEqualByComparingTo("53.82");
        assertThat(response.sgstAmount()).isEqualByComparingTo("53.82");
        assertThat(response.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("1004.64");
        assertThat(response.invoiceDisplayNumber()).isEqualTo("INV/2026-27/000001");
        assertThat(response.placeOfSupplyStateCode()).isEqualTo("27");

        org.mockito.Mockito.verify(stockService)
                .recordSale(tenantId, variantId, new BigDecimal("3"), response.id(), userId);
    }

    @Test
    void createInvoice_differentStateFromTenant_appliesIgstOnly() {
        customerWithState("09");
        InvoiceRequest request = new InvoiceRequest(
                customerId, LocalDate.of(2026, 6, 20), null,
                List.of(new InvoiceLineItemRequest(variantId, new BigDecimal("2"), new BigDecimal("299.00"))));

        InvoiceResponse response = invoiceService.createInvoice(tenantId, userId, request);

        assertThat(response.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(response.sgstAmount()).isEqualByComparingTo("0.00");
        assertThat(response.igstAmount()).isEqualByComparingTo("71.76");
        assertThat(response.placeOfSupplyStateCode()).isEqualTo("09");
    }

    @Test
    void createInvoice_explicitPlaceOfSupplyOverride_takesPrecedenceOverCustomerState() {
        customerWithState("27");
        InvoiceRequest request = new InvoiceRequest(
                customerId, LocalDate.of(2026, 6, 20), "09",
                List.of(new InvoiceLineItemRequest(variantId, BigDecimal.ONE, new BigDecimal("100.00"))));

        InvoiceResponse response = invoiceService.createInvoice(tenantId, userId, request);

        assertThat(response.placeOfSupplyStateCode()).isEqualTo("09");
        assertThat(response.igstAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.cgstAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void createInvoice_unknownCustomer_throwsResourceNotFound() {
        when(customerRepository.findByIdAndTenantId(customerId, tenantId)).thenReturn(Optional.empty());
        InvoiceRequest request = new InvoiceRequest(
                customerId, LocalDate.of(2026, 6, 20), null,
                List.of(new InvoiceLineItemRequest(variantId, BigDecimal.ONE, BigDecimal.TEN)));

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(tenantId, userId, request));
    }

    @Test
    void createInvoice_unknownVariant_throwsResourceNotFound() {
        customerWithState("27");
        UUID unknownVariant = UUID.randomUUID();
        when(variantRepository.findByIdAndTenantId(unknownVariant, tenantId)).thenReturn(Optional.empty());
        InvoiceRequest request = new InvoiceRequest(
                customerId, LocalDate.of(2026, 6, 20), null,
                List.of(new InvoiceLineItemRequest(unknownVariant, BigDecimal.ONE, BigDecimal.TEN)));

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(tenantId, userId, request));
    }

    @Test
    void createInvoice_missingTenant_throwsIllegalState() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        InvoiceRequest request = new InvoiceRequest(
                customerId, LocalDate.of(2026, 6, 20), null,
                List.of(new InvoiceLineItemRequest(variantId, BigDecimal.ONE, BigDecimal.TEN)));

        assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(tenantId, userId, request));
    }

    @Test
    void getInvoice_unknownId_throwsResourceNotFound() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice(tenantId, invoiceId));
    }

    @Test
    void getInvoice_found_returnsResponseWithLineItems() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setCustomerId(customerId);
        invoice.setStatus(InvoiceStatus.FINAL);

        InvoiceLineItem lineItem = new InvoiceLineItem();
        lineItem.setVariantId(variantId);
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(BigDecimal.TEN);
        lineItem.setLineTotal(BigDecimal.TEN);

        when(invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(invoiceLineItemRepository.findAllByInvoiceId(invoiceId)).thenReturn(List.of(lineItem));

        InvoiceResponse response = invoiceService.getInvoice(tenantId, invoiceId);

        assertThat(response.id()).isEqualTo(invoiceId);
        assertThat(response.lineItems()).hasSize(1);
    }

    @Test
    void listInvoices_mapsEachInvoiceWithItsLineItems() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        when(invoiceRepository.findAllByTenantId(tenantId)).thenReturn(List.of(invoice));
        when(invoiceLineItemRepository.findAllByInvoiceId(invoice.getId())).thenReturn(List.of());

        List<InvoiceResponse> responses = invoiceService.listInvoices(tenantId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(invoice.getId());
    }
}
