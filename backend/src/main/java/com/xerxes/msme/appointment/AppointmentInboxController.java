package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentRequestResponse;
import com.xerxes.msme.appointment.dto.AppointmentStatusResponse;
import com.xerxes.msme.appointment.dto.PendingCountResponse;
import com.xerxes.msme.security.AuthContext;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A business's own appointment requests: the inbox, the badge count, and the decisions. */
@RestController
@RequestMapping("/api/appointments/requests")
public class AppointmentInboxController {

    private final AppointmentInboxService inboxService;
    private final AuthContext authContext;

    public AppointmentInboxController(AppointmentInboxService inboxService, AuthContext authContext) {
        this.inboxService = inboxService;
        this.authContext = authContext;
    }

    @GetMapping
    public List<AppointmentRequestResponse> list(@RequestParam(required = false) AppointmentStatus status) {
        return inboxService.list(authContext.current().tenantId(), status);
    }

    @GetMapping("/pending-count")
    public PendingCountResponse pendingCount() {
        return inboxService.pendingCount(authContext.current().tenantId());
    }

    @PostMapping("/{id}/approve")
    public AppointmentStatusResponse approve(@PathVariable UUID id) {
        return inboxService.approve(authContext.current().tenantId(), id);
    }

    @PostMapping("/{id}/reject")
    public AppointmentStatusResponse reject(@PathVariable UUID id) {
        return inboxService.reject(authContext.current().tenantId(), id);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentStatusResponse cancel(@PathVariable UUID id) {
        return inboxService.cancel(authContext.current().tenantId(), id);
    }
}
