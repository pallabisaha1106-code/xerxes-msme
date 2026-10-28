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
import com.xerxes.msme.admin.dto.AdminServiceCityResponse;
import com.xerxes.msme.admin.dto.CreateServiceCityRequest;
import com.xerxes.msme.admin.dto.UpdateServiceCityRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminServiceCityController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminServiceCityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminServiceLocationService locationService;

    @Test
    void list_returnsCitiesForState() throws Exception {
        when(locationService.listCities("27"))
                .thenReturn(List.of(new AdminServiceCityResponse(UUID.randomUUID(), "27", "Pune", 1, true)));

        mockMvc.perform(get("/api/admin/service-cities").param("stateCode", "27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pune"));
    }

    @Test
    void create_delegatesToService() throws Exception {
        CreateServiceCityRequest request = new CreateServiceCityRequest("27", "Pune", 1);
        when(locationService.createCity(eq(request)))
                .thenReturn(new AdminServiceCityResponse(UUID.randomUUID(), "27", "Pune", 1, true));

        mockMvc.perform(post("/api/admin/service-cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pune"));
    }

    @Test
    void update_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateServiceCityRequest request = new UpdateServiceCityRequest("Pune City", 2, false);
        when(locationService.updateCity(eq(id), any())).thenReturn(new AdminServiceCityResponse(id, "27", "Pune City", 2, false));

        mockMvc.perform(put("/api/admin/service-cities/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
