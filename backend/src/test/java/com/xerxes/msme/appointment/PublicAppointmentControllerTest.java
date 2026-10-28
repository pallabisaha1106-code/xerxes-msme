package com.xerxes.msme.appointment;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xerxes.msme.appointment.dto.AppointmentRequestedResponse;
import com.xerxes.msme.appointment.dto.AvailabilityResponse;
import com.xerxes.msme.appointment.dto.AvailableDayResponse;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicAppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicAppointmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PublicAppointmentService publicAppointmentService;
    @MockBean private AppointmentBookingService bookingService;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void availability_returnsTheLengthAndTheDatesWithTheirTimes() throws Exception {
        when(publicAppointmentService.availability(tenantId)).thenReturn(new AvailabilityResponse(
                30, List.of(new AvailableDayResponse(LocalDate.of(2026, 10, 21), List.of("10:00", "10:30")))));

        mockMvc.perform(get("/api/public/appointments/" + tenantId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotMinutes").value(30))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-21"))
                .andExpect(jsonPath("$.days[0].times[1]").value("10:30"));
    }

    @Test
    void availability_noDates_isAnEmptyListNotAnError() throws Exception {
        when(publicAppointmentService.availability(tenantId)).thenReturn(new AvailabilityResponse(45, List.of()));

        mockMvc.perform(get("/api/public/appointments/" + tenantId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").isEmpty());
    }

    @Test
    void availability_aShopThatCannotTakeAppointments_isNotFound() throws Exception {
        when(publicAppointmentService.availability(tenantId)).thenThrow(ResourceNotFoundException.of("MarketplaceListing", tenantId));

        mockMvc.perform(get("/api/public/appointments/" + tenantId + "/availability")).andExpect(status().isNotFound());
    }

    @Test
    void availability_aMalformedShopId_isABadRequest() throws Exception {
        mockMvc.perform(get("/api/public/appointments/not-a-uuid/availability")).andExpect(status().isBadRequest());
    }

    private static final String VALID_REQUEST = """
            {"variantId":"%s","date":"2026-10-21","time":"10:30","name":"Asha","phone":"98765 43210","note":"first visit"}""";

    @Test
    void request_createsItAndAnswersWithASummary() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(bookingService.request(org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AppointmentRequestedResponse("Haircut (Men)", LocalDate.of(2026, 10, 21), "10:30", 30));

        mockMvc.perform(post("/api/public/appointments/" + tenantId + "/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.formatted(variantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("Haircut (Men)"))
                .andExpect(jsonPath("$.date").value("2026-10-21"))
                .andExpect(jsonPath("$.time").value("10:30"))
                .andExpect(jsonPath("$.slotMinutes").value(30));
    }

    @Test
    void request_badInput_isRejectedBeforeReachingTheService() throws Exception {
        String variant = UUID.randomUUID().toString();
        for (String body : List.of(
                "{}",
                "{\"variantId\":\"" + variant + "\",\"date\":\"2026-10-21\",\"time\":\"10:30\",\"name\":\"\",\"phone\":\"9876543210\"}",
                "{\"variantId\":\"" + variant + "\",\"date\":\"2026-10-21\",\"time\":\"10am\",\"name\":\"Asha\",\"phone\":\"9876543210\"}",
                "{\"variantId\":\"" + variant + "\",\"date\":\"21/10/2026\",\"time\":\"10:30\",\"name\":\"Asha\",\"phone\":\"9876543210\"}",
                "{\"variantId\":\"not-a-uuid\",\"date\":\"2026-10-21\",\"time\":\"10:30\",\"name\":\"Asha\",\"phone\":\"9876543210\"}",
                "{\"variantId\":\"" + variant + "\",\"date\":\"2026-10-21\",\"time\":\"10:30\",\"name\":\"Asha\",\"phone\":\"9876543210\",\"note\":\""
                        + "x".repeat(201) + "\"}")) {
            mockMvc.perform(post("/api/public/appointments/" + tenantId + "/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        org.mockito.Mockito.verify(bookingService, org.mockito.Mockito.never()).request(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void request_aRuleRefusal_isReportedAsUnprocessable_withItsMessage() throws Exception {
        when(bookingService.request(org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessRuleViolationException("That time is no longer available. Please pick another."));

        mockMvc.perform(post("/api/public/appointments/" + tenantId + "/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("That time is no longer available. Please pick another."));
    }

    @Test
    void request_aShopThatCannotTakeAppointments_isNotFound() throws Exception {
        when(bookingService.request(org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(ResourceNotFoundException.of("MarketplaceListing", tenantId));

        mockMvc.perform(post("/api/public/appointments/" + tenantId + "/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
