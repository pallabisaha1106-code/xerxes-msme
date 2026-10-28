package com.xerxes.msme.appointment;

import com.xerxes.msme.appointment.dto.AppointmentSettingsResponse;
import com.xerxes.msme.appointment.dto.UpdateAppointmentSettingsRequest;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A business's own appointment settings (how long each appointment is). */
@RestController
@RequestMapping("/api/appointments/settings")
public class AppointmentSettingsController {

    private final AppointmentSettingsService settingsService;
    private final AuthContext authContext;

    public AppointmentSettingsController(AppointmentSettingsService settingsService, AuthContext authContext) {
        this.settingsService = settingsService;
        this.authContext = authContext;
    }

    @GetMapping
    public AppointmentSettingsResponse get() {
        return settingsService.get(authContext.current().tenantId());
    }

    @PutMapping
    public AppointmentSettingsResponse update(@Valid @RequestBody UpdateAppointmentSettingsRequest request) {
        return settingsService.update(authContext.current().tenantId(), request.slotMinutes());
    }
}
