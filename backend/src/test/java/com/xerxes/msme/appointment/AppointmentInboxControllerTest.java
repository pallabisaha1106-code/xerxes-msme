package com.xerxes.msme.appointment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.appointment.dto.AppointmentRequestResponse;
import com.xerxes.msme.appointment.dto.AppointmentStatusResponse;
import com.xerxes.msme.appointment.dto.PendingCountResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AppointmentInboxController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentInboxControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AppointmentInboxService inboxService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    private AppointmentRequestResponse row() {
        return new AppointmentRequestResponse(id, "Haircut (Men)", new BigDecimal("300.00"), "Asha", "9876543210", "first visit",
                LocalDate.of(2026, 10, 21), "10:30", "11:00", AppointmentStatus.PENDING, 1, false, OffsetDateTime.parse("2026-10-19T04:30:00Z"));
    }

    @Test
    void list_returnsTheCallingBusinessesRequests_withoutAFilter() throws Exception {
        when(inboxService.list(tenantId, null)).thenReturn(List.of(row()));

        mockMvc.perform(get("/api/appointments/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("Haircut (Men)"))
                .andExpect(jsonPath("$[0].customerPhone").value("9876543210"))
                .andExpect(jsonPath("$[0].date").value("2026-10-21"))
                .andExpect(jsonPath("$[0].time").value("10:30"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].othersAtSameTime").value(1));
    }

    @Test
    void list_passesTheStatusFilterThrough() throws Exception {
        when(inboxService.list(tenantId, AppointmentStatus.APPROVED)).thenReturn(List.of());

        mockMvc.perform(get("/api/appointments/requests").param("status", "APPROVED")).andExpect(status().isOk());

        verify(inboxService).list(tenantId, AppointmentStatus.APPROVED);
    }

    @Test
    void list_anUnknownStatus_isABadRequest() throws Exception {
        mockMvc.perform(get("/api/appointments/requests").param("status", "MAYBE")).andExpect(status().isBadRequest());
    }

    @Test
    void pendingCount_returnsTheBadgeNumber() throws Exception {
        when(inboxService.pendingCount(tenantId)).thenReturn(new PendingCountResponse(3));

        mockMvc.perform(get("/api/appointments/requests/pending-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(3));
    }

    @Test
    void approve_rejectAndCancel_eachActOnTheNamedRequestForTheCallingBusiness() throws Exception {
        when(inboxService.approve(tenantId, id)).thenReturn(new AppointmentStatusResponse(id, AppointmentStatus.APPROVED));
        when(inboxService.reject(tenantId, id)).thenReturn(new AppointmentStatusResponse(id, AppointmentStatus.REJECTED));
        when(inboxService.cancel(tenantId, id)).thenReturn(new AppointmentStatusResponse(id, AppointmentStatus.CANCELLED));

        mockMvc.perform(post("/api/appointments/requests/" + id + "/approve")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/appointments/requests/" + id + "/reject")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(post("/api/appointments/requests/" + id + "/cancel")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void approve_aClash_isReportedAsUnprocessable_withItsMessage() throws Exception {
        when(inboxService.approve(tenantId, id)).thenThrow(new BusinessRuleViolationException("This time is already booked. Reject this request instead."));

        mockMvc.perform(post("/api/appointments/requests/" + id + "/approve"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This time is already booked. Reject this request instead."));
    }

    @Test
    void anUnknownRequest_isNotFound_andAMalformedIdIsABadRequest() throws Exception {
        when(inboxService.reject(any(), any())).thenThrow(ResourceNotFoundException.of("Appointment", id));

        mockMvc.perform(post("/api/appointments/requests/" + id + "/reject")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/appointments/requests/not-a-uuid/approve")).andExpect(status().isBadRequest());
    }
}
