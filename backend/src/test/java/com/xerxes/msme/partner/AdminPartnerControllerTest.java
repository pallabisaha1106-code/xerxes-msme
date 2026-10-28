package com.xerxes.msme.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.partner.dto.AdminPartnerResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
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

@WebMvcTest(controllers = AdminPartnerController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPartnerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminPartnerService adminPartnerService;
    @MockBean private AuthContext authContext;

    private final UUID partnerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    private AdminPartnerResponse response(PartnerStatus status) {
        return new AdminPartnerResponse(partnerId, "Meera", "meera@example.com", "919876543210", status, List.of(), 0, null, null, null);
    }

    @BeforeEach
    void admin() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(adminId, UUID.randomUUID(), "admin@xerxes.internal", Role.SUPER_ADMIN, UUID.randomUUID()));
    }

    @Test
    void list_passesTheStatusFilterThrough() throws Exception {
        when(adminPartnerService.listPartners(PartnerStatus.APPLIED)).thenReturn(List.of(response(PartnerStatus.APPLIED)));

        mockMvc.perform(get("/api/admin/partners").param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Meera"))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));
    }

    @Test
    void approve_actsAsTheSignedInAdmin() throws Exception {
        when(adminPartnerService.approve(partnerId, adminId)).thenReturn(response(PartnerStatus.ACTIVE));

        mockMvc.perform(post("/api/admin/partners/{id}/approve", partnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void reject_needsAReason() throws Exception {
        mockMvc.perform(post("/api/admin/partners/{id}/reject", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        when(adminPartnerService.reject(partnerId, "Not a fit", adminId)).thenReturn(response(PartnerStatus.REJECTED));
        mockMvc.perform(post("/api/admin/partners/{id}/reject", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not a fit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void deactivateAndReactivate_delegate() throws Exception {
        when(adminPartnerService.deactivate(partnerId, adminId)).thenReturn(response(PartnerStatus.INACTIVE));
        when(adminPartnerService.reactivate(partnerId, adminId)).thenReturn(response(PartnerStatus.ACTIVE));

        mockMvc.perform(post("/api/admin/partners/{id}/deactivate", partnerId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/partners/{id}/reactivate", partnerId)).andExpect(status().isOk());
    }

    @Test
    void replaceAreas_needsAtLeastOneArea() throws Exception {
        mockMvc.perform(put("/api/admin/partners/{id}/areas", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"areaIds\":[]}"))
                .andExpect(status().isBadRequest());

        UUID areaId = UUID.randomUUID();
        when(adminPartnerService.replaceAreas(eq(partnerId), any())).thenReturn(response(PartnerStatus.ACTIVE));
        mockMvc.perform(put("/api/admin/partners/{id}/areas", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"areaIds\":[\"" + areaId + "\"]}"))
                .andExpect(status().isOk());
        verify(adminPartnerService).replaceAreas(partnerId, List.of(areaId));
    }
}
