package com.xerxes.msme.partner;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.partner.dto.PartnerContactResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PartnerContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class PartnerContactControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PartnerContactService partnerContactService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void owner() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "owner@example.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void mine_withAPartner_returnsNameAndWhatsapp() throws Exception {
        when(partnerContactService.contactFor(tenantId))
                .thenReturn(Optional.of(new PartnerContactResponse("Meera", "919876543210")));

        mockMvc.perform(get("/api/partners/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Meera"))
                .andExpect(jsonPath("$.whatsapp").value("919876543210"));
    }

    @Test
    void mine_withoutAPartner_returns204() throws Exception {
        when(partnerContactService.contactFor(tenantId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/partners/mine")).andExpect(status().isNoContent());
    }
}
