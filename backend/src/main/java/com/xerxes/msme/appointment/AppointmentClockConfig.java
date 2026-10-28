package com.xerxes.msme.appointment;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The clock appointment code reads "now" from, so tests can pass a fixed one instead. */
@Configuration
public class AppointmentClockConfig {

    @Bean
    public Clock appointmentClock() {
        return Clock.system(AppointmentRules.ZONE);
    }
}
