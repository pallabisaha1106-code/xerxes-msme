package com.xerxes.msme.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.inventory.dto.AdjustmentRequest;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StockController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StockService stockService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void ledger_returnsMovementsForVariant() throws Exception {
        UUID variantId = UUID.randomUUID();
        StockLedger ledger = new StockLedger();
        ledger.setId(UUID.randomUUID());
        ledger.setVariantId(variantId);
        ledger.setMovementType(MovementType.PURCHASE);
        ledger.setQuantityChange(BigDecimal.TEN);
        ledger.setBalanceAfter(BigDecimal.TEN);
        when(stockService.getLedger(tenantId, variantId)).thenReturn(List.of(ledger));

        mockMvc.perform(get("/api/stock/variants/" + variantId + "/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("PURCHASE"));
    }

    @Test
    void adjust_validRequest_returns201() throws Exception {
        UUID variantId = UUID.randomUUID();
        StockLedger ledger = new StockLedger();
        ledger.setId(UUID.randomUUID());
        ledger.setVariantId(variantId);
        ledger.setMovementType(MovementType.ADJUSTMENT);
        ledger.setQuantityChange(new BigDecimal("5"));
        ledger.setBalanceAfter(new BigDecimal("55"));
        when(stockService.recordAdjustment(eq(tenantId), eq(variantId), any(), any(), eq(userId)))
                .thenReturn(ledger);

        mockMvc.perform(post("/api/stock/variants/" + variantId + "/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustmentRequest(new BigDecimal("5"), "count correction"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("ADJUSTMENT"));
    }

    @Test
    void adjust_missingSignedDelta_returns400() throws Exception {
        UUID variantId = UUID.randomUUID();

        mockMvc.perform(post("/api/stock/variants/" + variantId + "/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustmentRequest(null, "no delta"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lowStock_returnsVariantsBelowThreshold() throws Exception {
        com.xerxes.msme.catalog.ProductVariant variant = new com.xerxes.msme.catalog.ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setSku("SKU-1");
        variant.setCurrentStock(new BigDecimal("2"));
        variant.setReorderThreshold(new BigDecimal("5"));
        when(stockService.getLowStockVariants(tenantId)).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/stock/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1"));
    }
}
