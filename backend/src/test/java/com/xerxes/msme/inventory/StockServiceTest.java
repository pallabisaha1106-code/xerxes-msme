package com.xerxes.msme.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.storefront.StorefrontProperties;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private StockLedgerRepository stockLedgerRepository;

    private StockService stockService;

    private UUID tenantId;
    private UUID variantId;
    private UUID userId;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        stockService = new StockService(variantRepository, stockLedgerRepository, new StorefrontProperties(null, true));
        tenantId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        variant = new ProductVariant();
        variant.setId(variantId);
        variant.setTenantId(tenantId);
        variant.setSku("SKU-1");
        variant.setCurrentStock(new BigDecimal("50"));
    }

    private void stubVariantLookup() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.of(variant));
        lenient().when(stockLedgerRepository.save(any(StockLedger.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recordPurchase_increasesStockAndWritesPositiveLedgerEntry() {
        stubVariantLookup();
        UUID referenceId = UUID.randomUUID();

        StockLedger ledger = stockService.recordPurchase(tenantId, variantId, new BigDecimal("20"), referenceId, userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("70");
        assertThat(ledger.getMovementType()).isEqualTo(MovementType.PURCHASE);
        assertThat(ledger.getQuantityChange()).isEqualByComparingTo("20");
        assertThat(ledger.getBalanceAfter()).isEqualByComparingTo("70");
        assertThat(ledger.getReferenceType()).isEqualTo("PURCHASE_ENTRY");
        assertThat(ledger.getReferenceId()).isEqualTo(referenceId);
        verify(variantRepository).save(variant);
    }

    @Test
    void recordSale_decreasesStockAndWritesNegativeLedgerEntry() {
        stubVariantLookup();
        UUID referenceId = UUID.randomUUID();

        StockLedger ledger = stockService.recordSale(tenantId, variantId, new BigDecimal("15"), referenceId, userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("35");
        assertThat(ledger.getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(ledger.getQuantityChange()).isEqualByComparingTo("-15");
        assertThat(ledger.getBalanceAfter()).isEqualByComparingTo("35");
        assertThat(ledger.getReferenceType()).isEqualTo("INVOICE");
    }

    @Test
    void recordSale_exceedingAvailableStock_throwsAndDoesNotPersistAnything() {
        stubVariantLookup();

        assertThrows(
                BusinessRuleViolationException.class,
                () -> stockService.recordSale(tenantId, variantId, new BigDecimal("999"), UUID.randomUUID(), userId));

        verify(variantRepository, never()).save(any());
        verify(stockLedgerRepository, never()).save(any());
    }

    @Test
    void recordSale_exceedingAvailableStock_withStockCheckDisabled_isAllowedAndGoesNegative() {
        StockService lenientStockService =
                new StockService(variantRepository, stockLedgerRepository, new StorefrontProperties(null, false));
        stubVariantLookup();

        StockLedger ledger =
                lenientStockService.recordSale(tenantId, variantId, new BigDecimal("999"), UUID.randomUUID(), userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("-949");
        assertThat(ledger.getBalanceAfter()).isEqualByComparingTo("-949");
        verify(variantRepository).save(variant);
    }

    @Test
    void recordSale_exactlyDepletingStock_isAllowed() {
        stubVariantLookup();

        StockLedger ledger =
                stockService.recordSale(tenantId, variantId, new BigDecimal("50"), UUID.randomUUID(), userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("0");
        assertThat(ledger.getBalanceAfter()).isEqualByComparingTo("0");
    }

    @Test
    void recordPurchaseReturn_decreasesStock() {
        stubVariantLookup();

        StockLedger ledger = stockService.recordPurchaseReturn(
                tenantId, variantId, new BigDecimal("10"), UUID.randomUUID(), userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("40");
        assertThat(ledger.getMovementType()).isEqualTo(MovementType.PURCHASE_RETURN);
        assertThat(ledger.getQuantityChange()).isEqualByComparingTo("-10");
    }

    @Test
    void recordSaleReturn_increasesStock() {
        stubVariantLookup();

        StockLedger ledger =
                stockService.recordSaleReturn(tenantId, variantId, new BigDecimal("5"), UUID.randomUUID(), userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("55");
        assertThat(ledger.getMovementType()).isEqualTo(MovementType.SALE_RETURN);
        assertThat(ledger.getQuantityChange()).isEqualByComparingTo("5");
    }

    @Test
    void recordAdjustment_positiveDelta_increasesStockWithNullReferenceType() {
        stubVariantLookup();

        StockLedger ledger =
                stockService.recordAdjustment(tenantId, variantId, new BigDecimal("3"), "stock count correction", userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("53");
        assertThat(ledger.getMovementType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(ledger.getReferenceType()).isNull();
        assertThat(ledger.getRemarks()).isEqualTo("stock count correction");
    }

    @Test
    void recordAdjustment_negativeDelta_decreasesStock() {
        stubVariantLookup();

        StockLedger ledger =
                stockService.recordAdjustment(tenantId, variantId, new BigDecimal("-8"), "damaged goods", userId);

        assertThat(variant.getCurrentStock()).isEqualByComparingTo("42");
    }

    @Test
    void recordAdjustment_zeroDelta_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> stockService.recordAdjustment(tenantId, variantId, BigDecimal.ZERO, "noop", userId));
        verify(variantRepository, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void recordAdjustment_nullDelta_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> stockService.recordAdjustment(tenantId, variantId, null, "noop", userId));
    }

    @Test
    void recordPurchase_zeroOrNegativeQuantity_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> stockService.recordPurchase(tenantId, variantId, BigDecimal.ZERO, UUID.randomUUID(), userId));
        assertThrows(
                IllegalArgumentException.class,
                () -> stockService.recordPurchase(
                        tenantId, variantId, new BigDecimal("-1"), UUID.randomUUID(), userId));
    }

    @Test
    void movementForUnknownVariant_throwsResourceNotFound() {
        when(variantRepository.findByIdAndTenantId(variantId, tenantId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockService.recordPurchase(tenantId, variantId, BigDecimal.TEN, UUID.randomUUID(), userId));
    }

    @Test
    void getLedger_delegatesToRepositoryOrderedByRecency() {
        stockService.getLedger(tenantId, variantId);
        verify(stockLedgerRepository).findAllByTenantIdAndVariantIdOrderByCreatedAtDesc(tenantId, variantId);
    }

    @Test
    void getLowStockVariants_delegatesToRepository() {
        stockService.getLowStockVariants(tenantId);
        verify(variantRepository).findLowStock(tenantId);
    }

    @Test
    void ledgerEntry_capturesVariantIdAndCreatedBy() {
        stubVariantLookup();
        ArgumentCaptor<StockLedger> captor = ArgumentCaptor.forClass(StockLedger.class);

        stockService.recordPurchase(tenantId, variantId, BigDecimal.ONE, UUID.randomUUID(), userId);

        verify(stockLedgerRepository).save(captor.capture());
        assertThat(captor.getValue().getVariantId()).isEqualTo(variantId);
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(userId);
    }
}
