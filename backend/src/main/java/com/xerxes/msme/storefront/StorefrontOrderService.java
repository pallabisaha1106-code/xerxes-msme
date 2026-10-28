package com.xerxes.msme.storefront;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.sales.Customer;
import com.xerxes.msme.sales.CustomerRepository;
import com.xerxes.msme.sales.InvoiceService;
import com.xerxes.msme.sales.dto.InvoiceLineItemRequest;
import com.xerxes.msme.sales.dto.InvoiceRequest;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.storefront.dto.StorefrontOrderLineItemRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderLineItemResponse;
import com.xerxes.msme.storefront.dto.StorefrontOrderRequest;
import com.xerxes.msme.storefront.dto.StorefrontOrderResponse;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest order request lifecycle: NEW (just submitted, unreviewed) ->
 * CONFIRMED (owner reviewed, a real Customer + Invoice now exist for it) or
 * REJECTED (owner declined - out of stock, unreachable customer, etc). No
 * stock moves and no money changes hands until CONFIRMED.
 */
@Service
public class StorefrontOrderService {

    private final StorefrontOrderRepository orderRepository;
    private final StorefrontOrderLineItemRepository lineItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final InvoiceService invoiceService;

    public StorefrontOrderService(
            StorefrontOrderRepository orderRepository,
            StorefrontOrderLineItemRepository lineItemRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CustomerRepository customerRepository,
            TenantRepository tenantRepository,
            InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.lineItemRepository = lineItemRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.customerRepository = customerRepository;
        this.tenantRepository = tenantRepository;
        this.invoiceService = invoiceService;
    }

    private record ResolvedLine(ProductVariant variant, BigDecimal quantity, BigDecimal lineTotal) {}

    @Transactional
    public StorefrontOrderResponse submit(UUID tenantId, StorefrontOrderRequest request) {
        List<ResolvedLine> resolved = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (StorefrontOrderLineItemRequest lineRequest : request.lineItems()) {
            ProductVariant variant = variantRepository
                    .findByIdAndTenantId(lineRequest.variantId(), tenantId)
                    .filter(ProductVariant::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active product found for one of the requested items."));
            BigDecimal lineTotal = money(lineRequest.quantity().multiply(variant.getSellingPrice()));
            resolved.add(new ResolvedLine(variant, lineRequest.quantity(), lineTotal));
            subtotal = subtotal.add(lineTotal);
        }

        StorefrontOrder order = new StorefrontOrder();
        order.setTenantId(tenantId);
        order.setCustomerName(request.customerName());
        order.setCustomerPhone(request.customerPhone());
        order.setCustomerEmail(request.customerEmail());
        order.setAddressLine1(request.addressLine1());
        order.setAddressLine2(request.addressLine2());
        order.setCity(request.city());
        order.setPincode(request.pincode());
        order.setStateCode(request.stateCode());
        order.setNotes(request.notes());
        order.setSubtotal(subtotal);
        order = orderRepository.save(order);

        for (ResolvedLine line : resolved) {
            StorefrontOrderLineItem item = new StorefrontOrderLineItem();
            item.setTenantId(tenantId);
            item.setOrderId(order.getId());
            item.setVariantId(line.variant().getId());
            item.setQuantity(line.quantity());
            item.setUnitPrice(line.variant().getSellingPrice());
            item.setLineTotal(line.lineTotal());
            lineItemRepository.save(item);
        }

        return toResponse(tenantId, order);
    }

    @Transactional(readOnly = true)
    public List<StorefrontOrderResponse> list(UUID tenantId, StorefrontOrderStatus status) {
        List<StorefrontOrder> orders = status != null
                ? orderRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status)
                : orderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        return orders.stream().map(order -> toResponse(tenantId, order)).toList();
    }

    @Transactional(readOnly = true)
    public StorefrontOrderResponse get(UUID tenantId, UUID orderId) {
        return toResponse(tenantId, findOrder(tenantId, orderId));
    }

    @Transactional
    public StorefrontOrderResponse confirm(UUID tenantId, UUID orderId, UUID userId) {
        StorefrontOrder order = findOrder(tenantId, orderId);
        if (order.getStatus() != StorefrontOrderStatus.NEW) {
            throw new BusinessRuleViolationException("Only a new order can be confirmed.");
        }

        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated request references a missing tenant"));
        String placeOfSupplyStateCode = order.getStateCode() != null ? order.getStateCode() : tenant.getStateCode();

        Customer customer = customerRepository
                .findFirstByTenantIdAndPhone(tenantId, order.getCustomerPhone())
                .orElseGet(() -> {
                    Customer created = new Customer();
                    created.setTenantId(tenantId);
                    created.setName(order.getCustomerName());
                    created.setPhone(order.getCustomerPhone());
                    created.setEmail(order.getCustomerEmail());
                    created.setAddressLine1(order.getAddressLine1());
                    created.setAddressLine2(order.getAddressLine2());
                    created.setCity(order.getCity());
                    created.setPincode(order.getPincode());
                    created.setStateCode(placeOfSupplyStateCode);
                    return customerRepository.save(created);
                });

        List<InvoiceLineItemRequest> invoiceLineItems = lineItemRepository.findAllByOrderId(orderId).stream()
                .map(li -> new InvoiceLineItemRequest(li.getVariantId(), li.getQuantity(), li.getUnitPrice()))
                .toList();
        InvoiceRequest invoiceRequest =
                new InvoiceRequest(customer.getId(), LocalDate.now(), placeOfSupplyStateCode, invoiceLineItems);
        InvoiceResponse invoice = invoiceService.createInvoice(tenantId, userId, invoiceRequest);

        order.setStatus(StorefrontOrderStatus.CONFIRMED);
        order.setConfirmedInvoiceId(invoice.id());
        orderRepository.save(order);

        return toResponse(tenantId, order);
    }

    @Transactional
    public StorefrontOrderResponse reject(UUID tenantId, UUID orderId, String reason) {
        StorefrontOrder order = findOrder(tenantId, orderId);
        if (order.getStatus() != StorefrontOrderStatus.NEW) {
            throw new BusinessRuleViolationException("Only a new order can be rejected.");
        }
        order.setStatus(StorefrontOrderStatus.REJECTED);
        order.setRejectionReason(reason);
        orderRepository.save(order);
        return toResponse(tenantId, order);
    }

    private StorefrontOrder findOrder(UUID tenantId, UUID orderId) {
        return orderRepository
                .findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("StorefrontOrder", orderId));
    }

    private StorefrontOrderResponse toResponse(UUID tenantId, StorefrontOrder order) {
        List<StorefrontOrderLineItemResponse> lineItems = lineItemRepository.findAllByOrderId(order.getId()).stream()
                .map(li -> toLineItemResponse(tenantId, li))
                .toList();

        return new StorefrontOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getAddressLine1(),
                order.getAddressLine2(),
                order.getCity(),
                order.getPincode(),
                order.getStateCode(),
                order.getNotes(),
                order.getSubtotal(),
                order.getRejectionReason(),
                order.getConfirmedInvoiceId(),
                order.getCreatedAt(),
                lineItems);
    }

    private StorefrontOrderLineItemResponse toLineItemResponse(UUID tenantId, StorefrontOrderLineItem lineItem) {
        ProductVariant variant =
                variantRepository.findByIdAndTenantId(lineItem.getVariantId(), tenantId).orElse(null);
        String productName = "Unknown";
        String sku = "-";
        if (variant != null) {
            sku = variant.getSku();
            Product product =
                    productRepository.findByIdAndTenantId(variant.getProductId(), tenantId).orElse(null);
            if (product != null) {
                productName = product.getName();
            }
        }
        return new StorefrontOrderLineItemResponse(
                lineItem.getVariantId(), productName, sku, lineItem.getQuantity(), lineItem.getUnitPrice(),
                lineItem.getLineTotal());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
