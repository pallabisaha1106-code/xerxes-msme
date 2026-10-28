package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentOpenDaysResponse;
import com.xerxes.msme.appointment.dto.OpenDayResponse;
import com.xerxes.msme.appointment.dto.PublishNextRequest;
import com.xerxes.msme.appointment.dto.SetOpenDayRequest;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The dates a business has published for appointments: list the booking window, publish or unpublish one date. */
@RestController
@RequestMapping("/api/appointments/open-days")
public class AppointmentOpenDaysController {

    private final AppointmentOpenDaysService openDaysService;
    private final AuthContext authContext;

    public AppointmentOpenDaysController(AppointmentOpenDaysService openDaysService, AuthContext authContext) {
        this.openDaysService = openDaysService;
        this.authContext = authContext;
    }

    @GetMapping
    public AppointmentOpenDaysResponse list() {
        return openDaysService.list(authContext.current().tenantId());
    }

    /** The "publish the next week" shortcut: opens every coming date whose weekday has hours. */
    @PostMapping("/publish-next")
    public AppointmentOpenDaysResponse publishNext(@Valid @RequestBody PublishNextRequest request) {
        return openDaysService.publishNext(authContext.current().tenantId(), request.days());
    }

    @PutMapping("/{date}")
    public OpenDayResponse setOpen(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody SetOpenDayRequest request) {
        return openDaysService.setOpen(authContext.current().tenantId(), date, request.open());
    }
}
