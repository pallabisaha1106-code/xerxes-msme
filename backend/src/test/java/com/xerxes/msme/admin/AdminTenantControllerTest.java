package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminTenantResponse;
import com.xerxes.msme.admin.dto.UpdateTenantPlanRequest;
import com.xerxes.msme.admin.dto.UpdateTenantStatusRequest;
import com.xerxes.msme.subscription.SubscriptionStatus;
import com.xerxes.msme.subscription.dto.PendingInvoiceResponse;
import com.xerxes.msme.tenant.MembershipType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminTenantController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTenantControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminTenantService adminTenantService;

    private final UUID tenantId = UUID.randomUUID();

    private AdminTenantResponse response(boolean active, MembershipType plan) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminTenantResponse(
                tenantId, "Veera Collections", "veera@test.com", "9876543210", null, "27", null, null, "Pune", null,
                plan, null, active, now, 4, 6, 8, 1, SubscriptionStatus.ACTIVE, null, now.plusMonths(1), null, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    void list_returnsAllVendors() throws Exception {
        when(adminTenantService.listTenants()).thenReturn(List.of(response(true, MembershipType.FREE)));

        mockMvc.perform(get("/api/admin/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessName").value("Veera Collections"));
    }

    @Test
    void get_returnsOneVendorProfile() throws Exception {
        when(adminTenantService.getTenant(tenantId)).thenReturn(response(true, MembershipType.FREE));

        mockMvc.perform(get("/api/admin/tenants/" + tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCount").value(4));
    }

    @Test
    void assignPartner_passesThePartnerOnAndAllowsNullToClearIt() throws Exception {
        UUID partnerId = UUID.randomUUID();
        when(adminTenantService.assignPartner(eq(tenantId), eq(partnerId))).thenReturn(response(true, MembershipType.FREE));
        when(adminTenantService.assignPartner(eq(tenantId), eq(null))).thenReturn(response(true, MembershipType.FREE));

        mockMvc.perform(put("/api/admin/tenants/" + tenantId + "/partner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partnerId\":\"" + partnerId + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/tenants/" + tenantId + "/partner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partnerId\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_deactivatesVendor() throws Exception {
        when(adminTenantService.updateStatus(eq(tenantId), eq(false))).thenReturn(response(false, MembershipType.FREE));

        mockMvc.perform(put("/api/admin/tenants/" + tenantId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTenantStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updatePlan_changesSubscriptionPlan() throws Exception {
        when(adminTenantService.updatePlan(eq(tenantId), eq(MembershipType.PLUS)))
                .thenReturn(response(true, MembershipType.PLUS));

        mockMvc.perform(put("/api/admin/tenants/" + tenantId + "/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTenantPlanRequest(MembershipType.PLUS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionPlan").value("PLUS"));
    }

    @Test
    void updatePlan_missingPlan_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/tenants/" + tenantId + "/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateSubscriptionInvoice_returnsPaymentLink() throws Exception {
        when(adminTenantService.generateSubscriptionInvoice(tenantId))
                .thenReturn(new PendingInvoiceResponse(99900, "https://rzp.io/i/abc", OffsetDateTime.now()));

        mockMvc.perform(post("/api/admin/tenants/" + tenantId + "/subscription/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").value("https://rzp.io/i/abc"))
                .andExpect(jsonPath("$.amountPaise").value(99900));
    }
}
