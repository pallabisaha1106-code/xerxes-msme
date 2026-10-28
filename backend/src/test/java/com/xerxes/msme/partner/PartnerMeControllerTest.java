package com.xerxes.msme.partner;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.partner.dto.AdminPartnerResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PartnerMeController.class)
@AutoConfigureMockMvc(addFilters = false)
class PartnerMeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PartnerContext partnerContext;
    @MockBean private AdminPartnerService adminPartnerService;

    @Test
    void me_returnsTheCurrentPartnersOwnProfile() throws Exception {
        UUID partnerId = UUID.randomUUID();
        Partner partner = new Partner();
        partner.setId(partnerId);
        when(partnerContext.current()).thenReturn(partner);
        when(adminPartnerService.getPartner(partnerId))
                .thenReturn(new AdminPartnerResponse(
                        partnerId, "Meera", "meera@example.com", "919876543210", PartnerStatus.ACTIVE, List.of(), 3, null, null, null));

        mockMvc.perform(get("/api/partner/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Meera"))
                .andExpect(jsonPath("$.assignedShopCount").value(3));
    }
}
