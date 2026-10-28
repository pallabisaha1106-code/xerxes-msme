package com.xerxes.msme.partner;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.partner.dto.AssignWaitingResponse;
import com.xerxes.msme.partner.dto.WaitingAreaResponse;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WaitingShopsController.class)
@AutoConfigureMockMvc(addFilters = false)
class WaitingShopsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private WaitingShopsService waitingShopsService;
    @MockBean private AuthContext authContext;

    private final UUID admin = UUID.randomUUID();
    private final UUID areaId = UUID.randomUUID();

    @BeforeEach
    void superAdmin() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(admin, UUID.randomUUID(), "admin@xerxes.internal", Role.SUPER_ADMIN, UUID.randomUUID()));
    }

    @Test
    void list_returnsTheAreasWithWaitingShops() throws Exception {
        when(waitingShopsService.list()).thenReturn(List.of(new WaitingAreaResponse(areaId, "Kothrud", "Pune", 638, 2)));

        mockMvc.perform(get("/api/admin/partner-waiting-areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].areaName").value("Kothrud"))
                .andExpect(jsonPath("$[0].waitingShops").value(638))
                .andExpect(jsonPath("$[0].activePartners").value(2));
    }

    @Test
    void assign_handsOutTheAreaAsTheSignedInAdmin() throws Exception {
        when(waitingShopsService.assign(areaId, admin)).thenReturn(new AssignWaitingResponse(638, 0));

        mockMvc.perform(post("/api/admin/partner-waiting-areas/" + areaId + "/assign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(638))
                .andExpect(jsonPath("$.stillWaiting").value(0));
    }
}
