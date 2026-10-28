package com.xerxes.msme.inventory;

import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.storefront.StorefrontProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every stock change goes through here: it's the only place that writes to
 * stock_ledger and keeps product_variants.current_stock in sync with it, and
 * - while app.storefront.stock-check-enabled is true - the only place that
 * enforces "stock can't go negative". While that flag is off (MVP default,
 * since inventory isn't managed through the portal yet), movements are
 * allowed to take current_stock negative rather than being rejected.
 */
@Service
public class StockService {

    private final ProductVariantRepository variantRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StorefrontProperties storefrontProperties;

    public StockService(
            ProductVariantRepository variantRepository,
            StockLedgerRepository stockLedgerRepository,
            StorefrontProperties storefrontProperties) {
        this.variantRepository = variantRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.storefrontProperties = storefrontProperties;
    }

    @Transactional
    public StockLedger recordPurchase(UUID tenantId, UUID variantId, BigDecimal quantity, UUID referenceId, UUID createdBy) {
        requirePositive(quantity);
        return applyMovement(tenantId, variantId, MovementType.PURCHASE, quantity, "PURCHASE_ENTRY", referenceId, null, createdBy);
    }

    @Transactional
    public StockLedger recordSale(UUID tenantId, UUID variantId, BigDecimal quantity, UUID referenceId, UUID createdBy) {
        requirePositive(quantity);
        return applyMovement(tenantId, variantId, MovementType.SALE, quantity.negate(), "INVOICE", referenceId, null, createdBy);
    }

    @Transactional
    public StockLedger recordPurchaseReturn(
            UUID tenantId, UUID variantId, BigDecimal quantity, UUID referenceId, UUID createdBy) {
        requirePositive(quantity);
        return applyMovement(
                tenantId, variantId, MovementType.PURCHASE_RETURN, quantity.negate(), "PURCHASE_ENTRY", referenceId, null, createdBy);
    }

    @Transactional
    public StockLedger recordSaleReturn(UUID tenantId, UUID variantId, BigDecimal quantity, UUID referenceId, UUID createdBy) {
        requirePositive(quantity);
        return applyMovement(tenantId, variantId, MovementType.SALE_RETURN, quantity, "INVOICE", referenceId, null, createdBy);
    }

    @Transactional
    public StockLedger recordAdjustment(UUID tenantId, UUID variantId, BigDecimal signedDelta, String remarks, UUID createdBy) {
        if (signedDelta == null || signedDelta.signum() == 0) {
            throw new IllegalArgumentException("Adjustment quantity must be non-zero");
        }
        return applyMovement(tenantId, variantId, MovementType.ADJUSTMENT, signedDelta, null, null, remarks, createdBy);
    }

    @Transactional(readOnly = true)
    public List<StockLedger> getLedger(UUID tenantId, UUID variantId) {
        return stockLedgerRepository.findAllByTenantIdAndVariantIdOrderByCreatedAtDesc(tenantId, variantId);
    }

    @Transactional(readOnly = true)
    public List<ProductVariant> getLowStockVariants(UUID tenantId) {
        return variantRepository.findLowStock(tenantId);
    }

    private StockLedger applyMovement(
            UUID tenantId,
            UUID variantId,
            MovementType type,
            BigDecimal signedDelta,
            String referenceType,
            UUID referenceId,
            String remarks,
            UUID createdBy) {

        ProductVariant variant = variantRepository
                .findByIdAndTenantId(variantId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", variantId));

        BigDecimal newBalance = variant.getCurrentStock().add(signedDelta);
        if (storefrontProperties.stockCheckEnabled() && newBalance.signum() < 0) {
            throw new BusinessRuleViolationException(
                    "Insufficient stock for SKU " + variant.getSku() + ": available " + variant.getCurrentStock()
                            + ", requested " + signedDelta.abs());
        }

        variant.setCurrentStock(newBalance);
        variantRepository.save(variant);

        StockLedger ledger = new StockLedger();
        ledger.setTenantId(tenantId);
        ledger.setVariantId(variantId);
        ledger.setMovementType(type);
        ledger.setQuantityChange(signedDelta);
        ledger.setBalanceAfter(newBalance);
        ledger.setReferenceType(referenceType);
        ledger.setReferenceId(referenceId);
        ledger.setRemarks(remarks);
        ledger.setCreatedBy(createdBy);
        return stockLedgerRepository.save(ledger);
    }

    private void requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
