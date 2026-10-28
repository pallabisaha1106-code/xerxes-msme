package com.xerxes.msme.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.sales.Customer;
import com.xerxes.msme.sales.CustomerRepository;
import com.xerxes.msme.sales.InvoiceService;
import com.xerxes.msme.sales.InvoiceStatus;
import com.xerxes.msme.sales.dto.InvoiceRequest;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.storefront.dto.StorefrontOrderLineItemRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorefrontOrderServiceTest {

    @Mock private StorefrontOrderRepository orderRepository;
    @Mock private StorefrontOrderLineItemRepository lineItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private InvoiceService invoiceService;

    private StorefrontOrderService orderService;
    private UUID tenantId;
    private UUID variantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        orderService = new StorefrontOrderService(
                orderRepository, lineItemRepository, productRepository, variantRepository, customerRepository,
                tenantRepository, invoiceService);
        tenantId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        productId = UUID.randomUUID();

        lenient().when(orderRepository.save(any(StorefrontOrder.class))).thenAnswer(inv -> {
            StorefrontOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });
        lenient()
                .when(lineItemRepository.save(any(StorefrontOrderLineItem.class)))
                .thenAnswer(inv -> {
                    StorefrontOrderLineItem li = inv.getArgument(0);
                    if (li.getId() == null) {
                        li.setId(UUID.randomUUID());
                    }
                    return li;
                });
        lenient().when(lineItemRepository.findAllByOrderId(any())).thenReturn(List.of());
    }

    private ProductVariant activeVariant(BigDecimal sellingPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTenantId(tenantId);
        variant.setProductId(productId);
        variant.setSku("TSHIRT-M");
        variant.setSellingPrice(sellingPrice);
        variant.setActive(true);
        return variant;
    }

    private StorefrontOrderRequest requestFor(UUID variantId, BigDecimal quantity) {
        return new StorefrontOrderRequest(
                "Jane Doe", "9876543210", "jane@example.com", "Line 1", null, "Pune", "411001", null, null,
                List.of(new StorefrontOrderLineItemRequest(variantId, quantity)));
    }

    @Test
    void submit_resolvesPriceServerSideFromCurrentVariantPrice_ignoringAnyClientSuppliedPrice() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId))
                .thenReturn(Optional.of(activeVariant(new BigDecimal("299.00"))));

        StorefrontOrderResponse response = orderService.submit(tenantId, requestFor(variantId, new BigDecimal("2")));

        assertThat(response.subtotal()).isEqualByComparingTo("598.00");
        assertThat(response.status()).isEqualTo(StorefrontOrderStatus.NEW);

        ArgumentCaptor<StorefrontOrderLineItem> lineCaptor = ArgumentCaptor.forClass(StorefrontOrderLineItem.class);
        verify(lineItemRepository).save(lineCaptor.capture());
        assertThat(lineCaptor.getValue().getUnitPrice()).isEqualByComparingTo("299.00");
        assertThat(lineCaptor.getValue().getLineTotal()).isEqualByComparingTo("598.00");
    }

    @Test
    void submit_variantNotFound_throwsResourceNotFound() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.submit(tenantId, requestFor(variantId, BigDecimal.ONE)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void submit_variantInactive_throwsResourceNotFound() {
        ProductVariant inactive = activeVariant(new BigDecimal("299.00"));
        inactive.setActive(false);
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(inactive));

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.submit(tenantId, requestFor(variantId, BigDecimal.ONE)));
    }

    @Test
    void list_noStatusFilter_returnsAllOrdersForTenant() {
        StorefrontOrder order = new StorefrontOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(StorefrontOrderStatus.NEW);
        when(orderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(order));

        List<StorefrontOrderResponse> result = orderService.list(tenantId, null);

        assertThat(result).hasSize(1);
        verify(orderRepository, never()).findAllByTenantIdAndStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void list_withStatusFilter_delegatesToFilteredQuery() {
        when(orderRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, StorefrontOrderStatus.CONFIRMED))
                .thenReturn(List.of());

        orderService.list(tenantId, StorefrontOrderStatus.CONFIRMED);

        verify(orderRepository).findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, StorefrontOrderStatus.CONFIRMED);
        verify(orderRepository, never()).findAllByTenantIdOrderByCreatedAtDesc(any());
    }

    @Test
    void get_orderNotFoundForTenant_throwsResourceNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.get(tenantId, orderId));
    }

    private StorefrontOrder newOrder(String stateCode) {
        StorefrontOrder order = new StorefrontOrder();
        order.setId(UUID.randomUUID());
        order.setTenantId(tenantId);
        order.setStatus(StorefrontOrderStatus.NEW);
        order.setCustomerName("Jane Doe");
        order.setCustomerPhone("9876543210");
        order.setStateCode(stateCode);
        order.setSubtotal(new BigDecimal("299.00"));
        return order;
    }

    private Tenant tenantWithStateCode(String stateCode) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setStateCode(stateCode);
        return tenant;
    }

    private InvoiceResponse fakeInvoiceResponse(UUID invoiceId) {
        return new InvoiceResponse(
                invoiceId, UUID.randomUUID(), "INV/2026-27/000001", LocalDate.now(), "27", new BigDecimal("299.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("299.00"), InvoiceStatus.FINAL,
                List.of());
    }

    @Test
    void confirm_existingCustomerByPhone_reusesCustomer_andCreatesInvoice() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StorefrontOrder order = newOrder(null);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWithStateCode("27")));

        Customer existing = new Customer();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(tenantId);
        existing.setPhone("9876543210");
        when(customerRepository.findFirstByTenantIdAndPhone(tenantId, "9876543210")).thenReturn(Optional.of(existing));

        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.createInvoice(eq(tenantId), eq(userId), any(InvoiceRequest.class)))
                .thenReturn(fakeInvoiceResponse(invoiceId));

        StorefrontOrderResponse response = orderService.confirm(tenantId, orderId, userId);

        assertThat(response.status()).isEqualTo(StorefrontOrderStatus.CONFIRMED);
        assertThat(response.confirmedInvoiceId()).isEqualTo(invoiceId);
        verify(customerRepository, never()).save(any());

        ArgumentCaptor<InvoiceRequest> invoiceCaptor = ArgumentCaptor.forClass(InvoiceRequest.class);
        verify(invoiceService).createInvoice(eq(tenantId), eq(userId), invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().customerId()).isEqualTo(existing.getId());
        assertThat(invoiceCaptor.getValue().placeOfSupplyStateCode()).isEqualTo("27");
    }

    @Test
    void confirm_noExistingCustomer_createsOneWithTenantStateCode_whenOrderHasNoStateCode() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StorefrontOrder order = newOrder(null);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWithStateCode("27")));
        when(customerRepository.findFirstByTenantIdAndPhone(tenantId, "9876543210")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(invoiceService.createInvoice(eq(tenantId), eq(userId), any(InvoiceRequest.class)))
                .thenReturn(fakeInvoiceResponse(UUID.randomUUID()));

        orderService.confirm(tenantId, orderId, userId);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getStateCode()).isEqualTo("27");
        assertThat(customerCaptor.getValue().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void confirm_orderHasOwnStateCode_usesItInsteadOfTenantStateCode() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StorefrontOrder order = newOrder("09");
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWithStateCode("27")));
        when(customerRepository.findFirstByTenantIdAndPhone(tenantId, "9876543210")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(invoiceService.createInvoice(eq(tenantId), eq(userId), any(InvoiceRequest.class)))
                .thenReturn(fakeInvoiceResponse(UUID.randomUUID()));

        orderService.confirm(tenantId, orderId, userId);

        ArgumentCaptor<InvoiceRequest> invoiceCaptor = ArgumentCaptor.forClass(InvoiceRequest.class);
        verify(invoiceService).createInvoice(eq(tenantId), eq(userId), invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().placeOfSupplyStateCode()).isEqualTo("09");
    }

    @Test
    void confirm_orderNotInNewStatus_throwsBusinessRuleViolation() {
        UUID orderId = UUID.randomUUID();
        StorefrontOrder order = newOrder("27");
        order.setStatus(StorefrontOrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThrows(
                BusinessRuleViolationException.class, () -> orderService.confirm(tenantId, orderId, UUID.randomUUID()));
        verify(invoiceService, never()).createInvoice(any(), any(), any());
    }

    @Test
    void reject_setsStatusAndReason() {
        UUID orderId = UUID.randomUUID();
        StorefrontOrder order = newOrder("27");
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        StorefrontOrderResponse response = orderService.reject(tenantId, orderId, "Out of stock");

        assertThat(response.status()).isEqualTo(StorefrontOrderStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Out of stock");
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void reject_orderNotInNewStatus_throwsBusinessRuleViolation() {
        UUID orderId = UUID.randomUUID();
        StorefrontOrder order = newOrder("27");
        order.setStatus(StorefrontOrderStatus.REJECTED);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThrows(
                BusinessRuleViolationException.class, () -> orderService.reject(tenantId, orderId, "too late"));
    }

    @Test
    void toResponse_resolvesProductNameAndSkuFromLineItemVariant() {
        ProductVariant variant = activeVariant(new BigDecimal("299.00"));
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));

        Product product = new Product();
        product.setId(productId);
        product.setName("Cotton T-Shirt");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        StorefrontOrder order = newOrder("27");
        StorefrontOrderLineItem lineItem = new StorefrontOrderLineItem();
        lineItem.setId(UUID.randomUUID());
        lineItem.setOrderId(order.getId());
        lineItem.setVariantId(variantId);
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(new BigDecimal("299.00"));
        lineItem.setLineTotal(new BigDecimal("299.00"));
        when(orderRepository.findByIdAndTenantId(order.getId(), tenantId)).thenReturn(Optional.of(order));
        when(lineItemRepository.findAllByOrderId(order.getId())).thenReturn(List.of(lineItem));

        StorefrontOrderResponse response = orderService.get(tenantId, order.getId());

        assertThat(response.lineItems()).hasSize(1);
        assertThat(response.lineItems().get(0).productName()).isEqualTo("Cotton T-Shirt");
        assertThat(response.lineItems().get(0).sku()).isEqualTo("TSHIRT-M");
    }

    @Test
    void toResponse_variantNoLongerExists_fallsBackToUnknownProductPlaceholder() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());

        StorefrontOrder order = newOrder("27");
        StorefrontOrderLineItem lineItem = new StorefrontOrderLineItem();
        lineItem.setId(UUID.randomUUID());
        lineItem.setOrderId(order.getId());
        lineItem.setVariantId(variantId);
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(new BigDecimal("299.00"));
        lineItem.setLineTotal(new BigDecimal("299.00"));
        when(orderRepository.findByIdAndTenantId(order.getId(), tenantId)).thenReturn(Optional.of(order));
        when(lineItemRepository.findAllByOrderId(order.getId())).thenReturn(List.of(lineItem));

        StorefrontOrderResponse response = orderService.get(tenantId, order.getId());

        assertThat(response.lineItems().get(0).productName()).isEqualTo("Unknown");
        assertThat(response.lineItems().get(0).sku()).isEqualTo("-");
    }
}
