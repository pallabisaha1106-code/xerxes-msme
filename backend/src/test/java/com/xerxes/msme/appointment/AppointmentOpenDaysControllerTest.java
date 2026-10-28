package com.xerxes.msme.appointment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.appointment.dto.AppointmentOpenDaysResponse;
import com.xerxes.msme.appointment.dto.OpenDayResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.time.LocalDate;
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

@WebMvcTest(controllers = AppointmentOpenDaysController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentOpenDaysControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AppointmentOpenDaysService openDaysService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 10, 21);

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void get_returnsTheWindowForTheCallingBusiness() throws Exception {
        when(openDaysService.list(tenantId)).thenReturn(new AppointmentOpenDaysResponse(List.of(
                new OpenDayResponse(date, true, true), new OpenDayResponse(date.plusDays(1), false, false))));

        mockMvc.perform(get("/api/appointments/open-days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].date").value("2026-10-21"))
                .andExpect(jsonPath("$.days[0].open").value(true))
                .andExpect(jsonPath("$.days[1].weekdayHasHours").value(false));
    }

    @Test
    void put_publishesTheDateInThePath() throws Exception {
        when(openDaysService.setOpen(tenantId, date, true)).thenReturn(new OpenDayResponse(date, true, true));

        mockMvc.perform(put("/api/appointments/open-days/2026-10-21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"open\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-21"))
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    void put_unpublishesWhenOpenIsFalse() throws Exception {
        when(openDaysService.setOpen(tenantId, date, false)).thenReturn(new OpenDayResponse(date, false, true));

        mockMvc.perform(put("/api/appointments/open-days/2026-10-21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"open\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false));
    }

    @Test
    void put_badInput_isRejectedBeforeReachingTheService() throws Exception {
        mockMvc.perform(put("/api/appointments/open-days/2026-10-21").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/appointments/open-days/not-a-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"open\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/appointments/open-days/21-10-2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"open\":true}"))
                .andExpect(status().isBadRequest());

        verify(openDaysService, never()).setOpen(any(), any(), anyBoolean());
    }

    @Test
    void put_dateOutsideTheWindow_isReportedAsUnprocessable() throws Exception {
        when(openDaysService.setOpen(eq(tenantId), eq(date), eq(true)))
                .thenThrow(new BusinessRuleViolationException("You can publish dates from 2026-10-19 to 2026-11-01 only."));

        mockMvc.perform(put("/api/appointments/open-days/2026-10-21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"open\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("You can publish dates from 2026-10-19 to 2026-11-01 only."));
    }

    @Test
    void publishNext_returnsTheUpdatedWindow() throws Exception {
        when(openDaysService.publishNext(tenantId, 7)).thenReturn(new AppointmentOpenDaysResponse(
                List.of(new OpenDayResponse(date, true, true))));

        mockMvc.perform(post("/api/appointments/open-days/publish-next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].open").value(true));
    }

    @Test
    void publishNext_badDayCounts_areRejectedBeforeReachingTheService() throws Exception {
        for (String body : List.of("{}", "{\"days\":0}", "{\"days\":15}")) {
            mockMvc.perform(post("/api/appointments/open-days/publish-next").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        verify(openDaysService, never()).publishNext(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void publishNext_noHoursYet_isReportedAsUnprocessable() throws Exception {
        when(openDaysService.publishNext(tenantId, 7)).thenThrow(new BusinessRuleViolationException("Set your weekly opening hours first."));

        mockMvc.perform(post("/api/appointments/open-days/publish-next")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":7}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Set your weekly opening hours first."));
    }
}
