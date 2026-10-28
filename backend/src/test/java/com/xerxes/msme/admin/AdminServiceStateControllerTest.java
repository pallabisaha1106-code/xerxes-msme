package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminServiceStateResponse;
import com.xerxes.msme.admin.dto.UpdateServiceStateStatusRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminServiceStateController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminServiceStateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminServiceStateService serviceStateService;

    @Test
    void list_returnsEveryState() throws Exception {
        when(serviceStateService.listAll()).thenReturn(List.of(new AdminServiceStateResponse("27", "Maharashtra", 1, true)));

        mockMvc.perform(get("/api/admin/service-states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maharashtra"));
    }

    @Test
    void updateStatus_delegatesToService() throws Exception {
        when(serviceStateService.updateStatus(eq("27"), eq(false)))
                .thenReturn(new AdminServiceStateResponse("27", "Maharashtra", 1, false));

        mockMvc.perform(put("/api/admin/service-states/27/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateServiceStateStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
