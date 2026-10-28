package com.xerxes.msme.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.appointment.dto.AppointmentHoursResponse;
import com.xerxes.msme.appointment.dto.OpeningRangeRequest;
import com.xerxes.msme.appointment.dto.OpeningRangeResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AppointmentHoursController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentHoursControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AppointmentHoursService hoursService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private static final String VALID_WEEK = """
            {"ranges":[{"dayOfWeek":1,"opensAt":"10:00","closesAt":"13:00"},
                       {"dayOfWeek":1,"opensAt":"17:00","closesAt":"20:30"}]}""";

    @Test
    void get_returnsTheWeekWithTimesAsHoursAndMinutes() throws Exception {
        when(hoursService.getWeek(tenantId)).thenReturn(new AppointmentHoursResponse(
                List.of(new OpeningRangeResponse(1, LocalTime.of(10, 0), LocalTime.of(13, 0)))));

        mockMvc.perform(get("/api/appointments/hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranges[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.ranges[0].opensAt").value("10:00"))
                .andExpect(jsonPath("$.ranges[0].closesAt").value("13:00"));
    }

    @Test
    void put_readsHoursAndMinutes_andSavesForTheCallingBusiness() throws Exception {
        when(hoursService.replaceWeek(eq(tenantId), any())).thenReturn(new AppointmentHoursResponse(List.of()));

        mockMvc.perform(put("/api/appointments/hours").contentType(MediaType.APPLICATION_JSON).content(VALID_WEEK))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OpeningRangeRequest>> sent = ArgumentCaptor.forClass(List.class);
        verify(hoursService).replaceWeek(eq(tenantId), sent.capture());
        assertThat(sent.getValue()).containsExactly(
                new OpeningRangeRequest(1, LocalTime.of(10, 0), LocalTime.of(13, 0)),
                new OpeningRangeRequest(1, LocalTime.of(17, 0), LocalTime.of(20, 30)));
    }

    @Test
    void put_emptyList_isAllowed_meaningClosedEveryDay() throws Exception {
        when(hoursService.replaceWeek(eq(tenantId), any())).thenReturn(new AppointmentHoursResponse(List.of()));

        mockMvc.perform(put("/api/appointments/hours").contentType(MediaType.APPLICATION_JSON).content("{\"ranges\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ranges").isEmpty());
    }

    @Test
    void put_badInput_isRejectedBeforeReachingTheService() throws Exception {
        for (String body : List.of(
                "{}",
                "{\"ranges\":[{\"dayOfWeek\":0,\"opensAt\":\"10:00\",\"closesAt\":\"13:00\"}]}",
                "{\"ranges\":[{\"dayOfWeek\":8,\"opensAt\":\"10:00\",\"closesAt\":\"13:00\"}]}",
                "{\"ranges\":[{\"dayOfWeek\":1,\"closesAt\":\"13:00\"}]}",
                "{\"ranges\":[{\"dayOfWeek\":1,\"opensAt\":\"10am\",\"closesAt\":\"13:00\"}]}")) {
            mockMvc.perform(put("/api/appointments/hours").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        verify(hoursService, never()).replaceWeek(any(), any());
    }

    @Test
    void put_weekTheServiceRefuses_isReportedAsUnprocessable() throws Exception {
        when(hoursService.replaceWeek(eq(tenantId), any()))
                .thenThrow(new BusinessRuleViolationException("Monday: opening ranges must not overlap."));

        mockMvc.perform(put("/api/appointments/hours").contentType(MediaType.APPLICATION_JSON).content(VALID_WEEK))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Monday: opening ranges must not overlap."));
    }
}
