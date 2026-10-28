package com.xerxes.msme.appointment;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
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

@WebMvcTest(controllers = AppointmentSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentSettingsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AppointmentSettingsService settingsService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void get_returnsTheCallingBusinessesSettings() throws Exception {
        when(settingsService.get(tenantId)).thenReturn(new AppointmentSettingsResponse(45, List.of(30, 45, 60)));

        mockMvc.perform(get("/api/appointments/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotMinutes").value(45))
                .andExpect(jsonPath("$.allowedSlotMinutes[1]").value(45));
    }

    @Test
    void put_savesForTheCallingBusiness() throws Exception {
        when(settingsService.update(eq(tenantId), eq(20))).thenReturn(new AppointmentSettingsResponse(20, List.of(20, 30)));

        mockMvc.perform(put("/api/appointments/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotMinutes\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotMinutes").value(20));
    }

    @Test
    void put_missingLength_isRejectedBeforeReachingTheService() throws Exception {
        mockMvc.perform(put("/api/appointments/settings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(settingsService, never()).update(eq(tenantId), anyInt());
    }

    @Test
    void put_lengthTheServiceRefuses_isReportedAsUnprocessable() throws Exception {
        when(settingsService.update(eq(tenantId), eq(25)))
                .thenThrow(new BusinessRuleViolationException("Appointment length must be one of [10, 15] minutes."));

        mockMvc.perform(put("/api/appointments/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotMinutes\":25}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Appointment length must be one of [10, 15] minutes."));
    }
}
