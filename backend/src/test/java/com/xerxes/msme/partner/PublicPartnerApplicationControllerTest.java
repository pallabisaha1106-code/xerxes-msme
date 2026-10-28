package com.xerxes.msme.partner;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.partner.dto.PartnerApplicationRequest;
import com.xerxes.msme.partner.dto.PartnerApplicationResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicPartnerApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPartnerApplicationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PartnerApplicationService partnerApplicationService;

    private final UUID areaId = UUID.randomUUID();

    private String body(String name, String email, String password, List<UUID> areaIds) throws Exception {
        return objectMapper.writeValueAsString(new PartnerApplicationRequest(name, "9876543210", email, password, areaIds));
    }

    @Test
    void apply_validRequest_returns201() throws Exception {
        when(partnerApplicationService.apply(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PartnerApplicationResponse(UUID.randomUUID(), "Meera"));

        mockMvc.perform(post("/api/public/partner-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Meera", "meera@example.com", "SecurePass123", List.of(areaId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Meera"));
    }

    @Test
    void apply_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/public/partner-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "meera@example.com", "SecurePass123", List.of(areaId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apply_emailWithoutARealDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/public/partner-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Meera", "meera@test", "SecurePass123", List.of(areaId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apply_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/public/partner-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Meera", "meera@example.com", "short", List.of(areaId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apply_noAreas_returns400() throws Exception {
        mockMvc.perform(post("/api/public/partner-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Meera", "meera@example.com", "SecurePass123", List.of())))
                .andExpect(status().isBadRequest());
    }
}
