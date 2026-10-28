package com.xerxes.msme.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.inventory.StockService;
import com.xerxes.msme.purchase.dto.PurchaseLineItemRequest;
import com.xerxes.msme.purchase.dto.PurchaseRequest;
import com.xerxes.msme.purchase.dto.PurchaseResponse;
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
class PurchaseServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PurchaseEntryRepository purchaseEntryRepository;
    @Mock private PurchaseLineItemRepository purchaseLineItemRepository;
    @Mock private StockService stockService;

    private PurchaseService purchaseService;

    private UUID tenantId;
    private UUID userId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(
                vendorRepository, variantRepository, productRepository, purchaseEntryRepository,
                purchaseLineItemRepository, stockService);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        lenient()
                .when(vendorRepository.findByIdAndTenantId(vendorId, tenantId))
                .thenReturn(Optional.of(new Vendor()));
        lenient().when(purchaseEntryRepository.save(any(PurchaseEntry.class))).thenAnswer(inv -> {
            PurchaseEntry entry = inv.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(UUID.randomUUID());
            }
            return entry;
        });
        lenient()
                .when(purchaseLineItemRepository.save(any(PurchaseLineItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private ProductVariant variantWithGstOverride(UUID productId, BigDecimal overrideRate) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProductId(productId);
        variant.setGstRate(overrideRate);
        return variant;
    }

    @Test
    void createPurchase_computesSubtotalTaxAndTotal_andIncreasesStockPerLine() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        Product product = new Product();
        product.setId(productId);
        product.setDefaultGstRate(new BigDecimal("12"));

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku("SKU-1");

        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        PurchaseRequest request = new PurchaseRequest(
                vendorId, "REF-001", LocalDate.of(2026, 6, 1),
                List.of(new PurchaseLineItemRequest(variantId, new BigDecimal("10"), new BigDecimal("100.00"))));

        PurchaseResponse response = purchaseService.createPurchase(tenantId, userId, request);

        // 10 * 100 = 1000 subtotal, 12% tax = 120, total = 1120
        assertThat(response.subtotal()).isEqualByComparingTo("1000.00");
        assertThat(response.totalTax()).isEqualByComparingTo("120.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("1120.00");
        assertThat(response.lineItems()).hasSize(1);
        assertThat(response.lineItems().get(0).gstRate()).isEqualByComparingTo("12");

        verify(stockService).recordPurchase(tenantId, variantId, new BigDecimal("10"), response.id(), userId);
    }

    @Test
    void createPurchase_variantGstOverride_takesPrecedenceOverProductDefault() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setDefaultGstRate(new BigDecimal("18"));

        ProductVariant variant = variantWithGstOverride(productId, new BigDecimal("5"));

        when(variantRepository.findByIdAndTenantId(variant.getId(), tenantId)).thenReturn(Optional.of(variant));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        PurchaseRequest request = new PurchaseRequest(
                vendorId, null, LocalDate.of(2026, 6, 1),
                List.of(new PurchaseLineItemRequest(variant.getId(), BigDecimal.TEN, new BigDecimal("50.00"))));

        PurchaseResponse response = purchaseService.createPurchase(tenantId, userId, request);

        assertThat(response.lineItems().get(0).gstRate()).isEqualByComparingTo("5");
        assertThat(response.totalTax()).isEqualByComparingTo("25.00");
    }

    @Test
    void createPurchase_multipleLineItems_summedAcrossLines() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setDefaultGstRate(new BigDecimal("10"));

        ProductVariant variantA = new ProductVariant();
        variantA.setId(UUID.randomUUID());
        variantA.setProductId(productId);
        ProductVariant variantB = new ProductVariant();
        variantB.setId(UUID.randomUUID());
        variantB.setProductId(productId);

        when(variantRepository.findByIdAndTenantId(variantA.getId(), tenantId)).thenReturn(Optional.of(variantA));
        when(variantRepository.findByIdAndTenantId(variantB.getId(), tenantId)).thenReturn(Optional.of(variantB));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        PurchaseRequest request = new PurchaseRequest(
                vendorId, null, LocalDate.of(2026, 6, 1),
                List.of(
                        new PurchaseLineItemRequest(variantA.getId(), BigDecimal.TEN, new BigDecimal("100.00")),
                        new PurchaseLineItemRequest(variantB.getId(), new BigDecimal("5"), new BigDecimal("200.00"))));

        PurchaseResponse response = purchaseService.createPurchase(tenantId, userId, request);

        // line1: 1000 sub + 100 tax; line2: 1000 sub + 100 tax => 2000 sub, 200 tax, 2200 total
        assertThat(response.subtotal()).isEqualByComparingTo("2000.00");
        assertThat(response.totalTax()).isEqualByComparingTo("200.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("2200.00");
    }

    @Test
    void createPurchase_unknownVendor_throwsResourceNotFound() {
        UUID unknownVendor = UUID.randomUUID();
        when(vendorRepository.findByIdAndTenantId(unknownVendor, tenantId)).thenReturn(Optional.empty());

        PurchaseRequest request = new PurchaseRequest(
                unknownVendor, null, LocalDate.now(),
                List.of(new PurchaseLineItemRequest(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN)));

        assertThrows(ResourceNotFoundException.class, () -> purchaseService.createPurchase(tenantId, userId, request));
    }

    @Test
    void createPurchase_unknownVariant_throwsResourceNotFound() {
        UUID unknownVariant = UUID.randomUUID();
        when(variantRepository.findByIdAndTenantId(unknownVariant, tenantId)).thenReturn(Optional.empty());

        PurchaseRequest request = new PurchaseRequest(
                vendorId, null, LocalDate.now(),
                List.of(new PurchaseLineItemRequest(unknownVariant, BigDecimal.ONE, BigDecimal.TEN)));

        assertThrows(ResourceNotFoundException.class, () -> purchaseService.createPurchase(tenantId, userId, request));
    }

    @Test
    void getPurchase_unknownId_throwsResourceNotFound() {
        UUID purchaseId = UUID.randomUUID();
        when(purchaseEntryRepository.findByIdAndTenantId(purchaseId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> purchaseService.getPurchase(tenantId, purchaseId));
    }

    @Test
    void getPurchase_found_returnsResponseWithLineItems() {
        UUID purchaseId = UUID.randomUUID();
        PurchaseEntry entry = new PurchaseEntry();
        entry.setId(purchaseId);
        entry.setVendorId(vendorId);
        entry.setSubtotal(new BigDecimal("100.00"));

        PurchaseLineItem lineItem = new PurchaseLineItem();
        lineItem.setVariantId(UUID.randomUUID());
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(new BigDecimal("100.00"));
        lineItem.setGstRate(BigDecimal.ZERO);
        lineItem.setLineTotal(new BigDecimal("100.00"));

        when(purchaseEntryRepository.findByIdAndTenantId(purchaseId, tenantId)).thenReturn(Optional.of(entry));
        when(purchaseLineItemRepository.findAllByPurchaseEntryId(purchaseId)).thenReturn(List.of(lineItem));

        PurchaseResponse response = purchaseService.getPurchase(tenantId, purchaseId);

        assertThat(response.id()).isEqualTo(purchaseId);
        assertThat(response.lineItems()).hasSize(1);
    }

    @Test
    void listPurchases_mapsEachEntryWithItsLineItems() {
        PurchaseEntry entry = new PurchaseEntry();
        entry.setId(UUID.randomUUID());
        when(purchaseEntryRepository.findAllByTenantId(tenantId)).thenReturn(List.of(entry));
        when(purchaseLineItemRepository.findAllByPurchaseEntryId(entry.getId())).thenReturn(List.of());

        List<PurchaseResponse> responses = purchaseService.listPurchases(tenantId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(entry.getId());
    }
}
