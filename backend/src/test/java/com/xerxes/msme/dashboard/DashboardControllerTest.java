package com.xerxes.msme.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.dashboard.dto.DashboardResponse;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DashboardService dashboardService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void get_returnsDashboardForCurrentTenant() throws Exception {
        DashboardResponse response = new DashboardResponse(
                new BigDecimal("50000.00"), new BigDecimal("1200.00"), new BigDecimal("5000.00"),
                new BigDecimal("20000.00"), 12, 30, 4, 3, List.of(), List.of());
        when(dashboardService.getDashboard(tenantId)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryValue").value(50000.00))
                .andExpect(jsonPath("$.totalProducts").value(12))
                .andExpect(jsonPath("$.lowStockCount").value(3));
    }
}
