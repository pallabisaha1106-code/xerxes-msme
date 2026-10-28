package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentRequestedResponse;
import com.xerxes.msme.appointment.dto.AvailabilityResponse;
import com.xerxes.msme.appointment.dto.CreateAppointmentRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated, rate-limited like the rest of /api/public/**: what a shopper sees when picking an appointment. */
@RestController
@RequestMapping("/api/public/appointments")
public class PublicAppointmentController {

    private final PublicAppointmentService publicAppointmentService;
    private final AppointmentBookingService bookingService;

    public PublicAppointmentController(PublicAppointmentService publicAppointmentService, AppointmentBookingService bookingService) {
        this.publicAppointmentService = publicAppointmentService;
        this.bookingService = bookingService;
    }

    @GetMapping("/{tenantId}/availability")
    public AvailabilityResponse availability(@PathVariable UUID tenantId) {
        return publicAppointmentService.availability(tenantId);
    }

    /** Sends a request for a slot; the business confirms it offline, so the answer is just a summary. */
    @PostMapping("/{tenantId}/requests")
    public ResponseEntity<AppointmentRequestedResponse> request(
            @PathVariable UUID tenantId, @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.request(tenantId, request));
    }
}
