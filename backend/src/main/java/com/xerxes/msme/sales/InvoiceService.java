package com.xerxes.msme.sales;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.catalog.VariantAttributeResolver;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.inventory.StockService;
import com.xerxes.msme.sales.GstSplitCalculator.GstSplit;
import com.xerxes.msme.sales.dto.InvoiceLineItemRequest;
import com.xerxes.msme.sales.dto.InvoiceLineItemResponse;
import com.xerxes.msme.sales.dto.InvoiceRequest;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recording an invoice decreases stock for each line item and burns one
 * atomically-allocated sequential invoice number. Money math is scaled to 2
 * decimal places at the point each figure is computed, matching the
 * NUMERIC(14,2) columns it lands in.
 */
@Service
public class InvoiceService {

    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final InvoiceNumberService invoiceNumberService;
    private final StockService stockService;

    public InvoiceService(
            TenantRepository tenantRepository,
            CustomerRepository customerRepository,
            ProductVariantRepository variantRepository,
            ProductRepository productRepository,
            InvoiceRepository invoiceRepository,
            InvoiceLineItemRepository invoiceLineItemRepository,
            InvoiceNumberService invoiceNumberService,
            StockService stockService) {
        this.tenantRepository = tenantRepository;
        this.customerRepository = customerRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.invoiceNumberService = invoiceNumberService;
        this.stockService = stockService;
    }

    @Transactional
    public InvoiceResponse createInvoice(UUID tenantId, UUID createdBy, InvoiceRequest request) {
        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated request references a missing tenant"));
        Customer customer = customerRepository
                .findByIdAndTenantId(request.customerId(), tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", request.customerId()));

        String placeOfSupplyStateCode =
                request.placeOfSupplyStateCode() != null ? request.placeOfSupplyStateCode() : customer.getStateCode();

        InvoiceNumberAllocation allocation = invoiceNumberService.allocateNext(tenantId, request.invoiceDate());

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setCustomerId(customer.getId());
        invoice.setFinancialYear(allocation.financialYear());
        invoice.setInvoiceNumber(allocation.invoiceNumber());
        invoice.setInvoiceDisplayNumber(allocation.displayNumber());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setPlaceOfSupplyStateCode(placeOfSupplyStateCode);
        invoice.setCreatedBy(createdBy);
        invoice = invoiceRepository.save(invoice);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        List<InvoiceLineItemResponse> lineItemResponses = new ArrayList<>();

        for (InvoiceLineItemRequest lineRequest : request.lineItems()) {
            ProductVariant variant = variantRepository
                    .findByIdAndTenantId(lineRequest.variantId(), tenantId)
                    .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", lineRequest.variantId()));
            Product product = productRepository
                    .findByIdAndTenantId(variant.getProductId(), tenantId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", variant.getProductId()));

            BigDecimal gstRate = VariantAttributeResolver.resolveGstRate(product, variant);
            BigDecimal lineSubtotal = money(lineRequest.quantity().multiply(lineRequest.unitPrice()));
            GstSplit split = GstSplitCalculator.calculate(
                    lineSubtotal, gstRate, tenant.getStateCode(), placeOfSupplyStateCode);
            BigDecimal lineTotal = lineSubtotal.add(split.cgst()).add(split.sgst()).add(split.igst());

            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setTenantId(tenantId);
            lineItem.setInvoiceId(invoice.getId());
            lineItem.setVariantId(variant.getId());
            lineItem.setQuantity(lineRequest.quantity());
            lineItem.setUnitPrice(lineRequest.unitPrice());
            lineItem.setGstRate(gstRate);
            lineItem.setCgstAmount(split.cgst());
            lineItem.setSgstAmount(split.sgst());
            lineItem.setIgstAmount(split.igst());
            lineItem.setLineTotal(lineTotal);
            invoiceLineItemRepository.save(lineItem);

            stockService.recordSale(tenantId, variant.getId(), lineRequest.quantity(), invoice.getId(), createdBy);

            subtotal = subtotal.add(lineSubtotal);
            totalCgst = totalCgst.add(split.cgst());
            totalSgst = totalSgst.add(split.sgst());
            totalIgst = totalIgst.add(split.igst());
            lineItemResponses.add(new InvoiceLineItemResponse(
                    variant.getId(),
                    lineRequest.quantity(),
                    lineRequest.unitPrice(),
                    gstRate,
                    split.cgst(),
                    split.sgst(),
                    split.igst(),
                    lineTotal));
        }

        invoice.setSubtotal(subtotal);
        invoice.setCgstAmount(totalCgst);
        invoice.setSgstAmount(totalSgst);
        invoice.setIgstAmount(totalIgst);
        invoice.setTotalAmount(subtotal.add(totalCgst).add(totalSgst).add(totalIgst));
        invoice = invoiceRepository.save(invoice);

        return toResponse(invoice, lineItemResponses);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID tenantId, UUID invoiceId) {
        Invoice invoice = invoiceRepository
                .findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", invoiceId));
        return toResponse(invoice, mapLineItems(invoice));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID tenantId) {
        return invoiceRepository.findAllByTenantId(tenantId).stream()
                .map(invoice -> toResponse(invoice, mapLineItems(invoice)))
                .toList();
    }

    private List<InvoiceLineItemResponse> mapLineItems(Invoice invoice) {
        return invoiceLineItemRepository.findAllByInvoiceId(invoice.getId()).stream()
                .map(li -> new InvoiceLineItemResponse(
                        li.getVariantId(),
                        li.getQuantity(),
                        li.getUnitPrice(),
                        li.getGstRate(),
                        li.getCgstAmount(),
                        li.getSgstAmount(),
                        li.getIgstAmount(),
                        li.getLineTotal()))
                .toList();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private InvoiceResponse toResponse(Invoice invoice, List<InvoiceLineItemResponse> lineItems) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getCustomerId(),
                invoice.getInvoiceDisplayNumber(),
                invoice.getInvoiceDate(),
                invoice.getPlaceOfSupplyStateCode(),
                invoice.getSubtotal(),
                invoice.getCgstAmount(),
                invoice.getSgstAmount(),
                invoice.getIgstAmount(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                lineItems);
    }
}
