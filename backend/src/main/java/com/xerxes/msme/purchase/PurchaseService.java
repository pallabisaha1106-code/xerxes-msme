package com.xerxes.msme.purchase;

import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.catalog.VariantAttributeResolver;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.inventory.StockService;
import com.xerxes.msme.purchase.dto.PurchaseLineItemRequest;
import com.xerxes.msme.purchase.dto.PurchaseLineItemResponse;
import com.xerxes.msme.purchase.dto.PurchaseRequest;
import com.xerxes.msme.purchase.dto.PurchaseResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recording a purchase increases stock for each line item. Money math is
 * scaled to 2 decimal places at the point each figure is computed, matching
 * the NUMERIC(14,2) columns it lands in.
 */
@Service
public class PurchaseService {

    private final VendorRepository vendorRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final PurchaseEntryRepository purchaseEntryRepository;
    private final PurchaseLineItemRepository purchaseLineItemRepository;
    private final StockService stockService;

    public PurchaseService(
            VendorRepository vendorRepository,
            ProductVariantRepository variantRepository,
            ProductRepository productRepository,
            PurchaseEntryRepository purchaseEntryRepository,
            PurchaseLineItemRepository purchaseLineItemRepository,
            StockService stockService) {
        this.vendorRepository = vendorRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.purchaseEntryRepository = purchaseEntryRepository;
        this.purchaseLineItemRepository = purchaseLineItemRepository;
        this.stockService = stockService;
    }

    @Transactional
    public PurchaseResponse createPurchase(UUID tenantId, UUID createdBy, PurchaseRequest request) {
        vendorRepository
                .findByIdAndTenantId(request.vendorId(), tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vendor", request.vendorId()));

        PurchaseEntry entry = new PurchaseEntry();
        entry.setTenantId(tenantId);
        entry.setVendorId(request.vendorId());
        entry.setReferenceNumber(request.referenceNumber());
        entry.setPurchaseDate(request.purchaseDate());
        entry.setCreatedBy(createdBy);
        entry = purchaseEntryRepository.save(entry);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<PurchaseLineItemResponse> lineItemResponses = new ArrayList<>();

        for (PurchaseLineItemRequest lineRequest : request.lineItems()) {
            ProductVariant variant = variantRepository
                    .findByIdAndTenantId(lineRequest.variantId(), tenantId)
                    .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", lineRequest.variantId()));
            Product product = productRepository
                    .findByIdAndTenantId(variant.getProductId(), tenantId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", variant.getProductId()));

            BigDecimal gstRate = VariantAttributeResolver.resolveGstRate(product, variant);
            BigDecimal lineSubtotal = money(lineRequest.quantity().multiply(lineRequest.unitPrice()));
            BigDecimal lineTax = money(lineSubtotal.multiply(gstRate).divide(BigDecimal.valueOf(100)));
            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            PurchaseLineItem lineItem = new PurchaseLineItem();
            lineItem.setTenantId(tenantId);
            lineItem.setPurchaseEntryId(entry.getId());
            lineItem.setVariantId(variant.getId());
            lineItem.setQuantity(lineRequest.quantity());
            lineItem.setUnitPrice(lineRequest.unitPrice());
            lineItem.setGstRate(gstRate);
            lineItem.setLineTotal(lineTotal);
            purchaseLineItemRepository.save(lineItem);

            stockService.recordPurchase(tenantId, variant.getId(), lineRequest.quantity(), entry.getId(), createdBy);

            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);
            lineItemResponses.add(new PurchaseLineItemResponse(
                    variant.getId(), lineRequest.quantity(), lineRequest.unitPrice(), gstRate, lineTotal));
        }

        entry.setSubtotal(subtotal);
        entry.setTotalTax(totalTax);
        entry.setTotalAmount(subtotal.add(totalTax));
        entry = purchaseEntryRepository.save(entry);

        return toResponse(entry, lineItemResponses);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchase(UUID tenantId, UUID purchaseId) {
        PurchaseEntry entry = purchaseEntryRepository
                .findByIdAndTenantId(purchaseId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("PurchaseEntry", purchaseId));
        List<PurchaseLineItemResponse> lineItems = purchaseLineItemRepository.findAllByPurchaseEntryId(entry.getId()).stream()
                .map(li -> new PurchaseLineItemResponse(
                        li.getVariantId(), li.getQuantity(), li.getUnitPrice(), li.getGstRate(), li.getLineTotal()))
                .toList();
        return toResponse(entry, lineItems);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> listPurchases(UUID tenantId) {
        return purchaseEntryRepository.findAllByTenantId(tenantId).stream()
                .map(entry -> {
                    List<PurchaseLineItemResponse> lineItems = purchaseLineItemRepository
                            .findAllByPurchaseEntryId(entry.getId())
                            .stream()
                            .map(li -> new PurchaseLineItemResponse(
                                    li.getVariantId(), li.getQuantity(), li.getUnitPrice(), li.getGstRate(), li.getLineTotal()))
                            .toList();
                    return toResponse(entry, lineItems);
                })
                .toList();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private PurchaseResponse toResponse(PurchaseEntry entry, List<PurchaseLineItemResponse> lineItems) {
        return new PurchaseResponse(
                entry.getId(),
                entry.getVendorId(),
                entry.getReferenceNumber(),
                entry.getPurchaseDate(),
                entry.getSubtotal(),
                entry.getTotalTax(),
                entry.getTotalAmount(),
                lineItems);
    }
}
