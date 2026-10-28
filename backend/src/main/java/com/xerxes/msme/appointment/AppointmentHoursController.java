package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentHoursResponse;
import com.xerxes.msme.appointment.dto.ReplaceHoursRequest;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A business's own weekly opening hours; PUT replaces the whole week. */
@RestController
@RequestMapping("/api/appointments/hours")
public class AppointmentHoursController {

    private final AppointmentHoursService hoursService;
    private final AuthContext authContext;

    public AppointmentHoursController(AppointmentHoursService hoursService, AuthContext authContext) {
        this.hoursService = hoursService;
        this.authContext = authContext;
    }

    @GetMapping
    public AppointmentHoursResponse get() {
        return hoursService.getWeek(authContext.current().tenantId());
    }

    @PutMapping
    public AppointmentHoursResponse replace(@Valid @RequestBody ReplaceHoursRequest request) {
        return hoursService.replaceWeek(authContext.current().tenantId(), request.ranges());
    }
}
