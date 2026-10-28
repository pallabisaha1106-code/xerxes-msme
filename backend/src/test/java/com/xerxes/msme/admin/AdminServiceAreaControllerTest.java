package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminServiceAreaResponse;
import com.xerxes.msme.admin.dto.CreateServiceAreaRequest;
import com.xerxes.msme.admin.dto.UpdateServiceAreaRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminServiceAreaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminServiceAreaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminServiceLocationService locationService;

    private final UUID cityId = UUID.randomUUID();

    @Test
    void list_returnsAreasForCity() throws Exception {
        when(locationService.listAreas(cityId))
                .thenReturn(List.of(new AdminServiceAreaResponse(UUID.randomUUID(), cityId, "Nigdi", "411044", 1, true)));

        mockMvc.perform(get("/api/admin/service-areas").param("cityId", cityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nigdi"));
    }

    @Test
    void create_delegatesToService() throws Exception {
        CreateServiceAreaRequest request = new CreateServiceAreaRequest(cityId, "Nigdi", "411044", 1);
        when(locationService.createArea(eq(request)))
                .thenReturn(new AdminServiceAreaResponse(UUID.randomUUID(), cityId, "Nigdi", "411044", 1, true));

        mockMvc.perform(post("/api/admin/service-areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nigdi"));
    }

    @Test
    void update_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateServiceAreaRequest request = new UpdateServiceAreaRequest("Nigdi Sector 26", "411044", 2, false);
        when(locationService.updateArea(eq(id), any()))
                .thenReturn(new AdminServiceAreaResponse(id, cityId, "Nigdi Sector 26", "411044", 2, false));

        mockMvc.perform(put("/api/admin/service-areas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
