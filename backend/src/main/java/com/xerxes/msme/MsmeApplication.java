package com.xerxes.msme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UserDetailsServiceAutoConfiguration is excluded because authentication here
 * is entirely JWT-based (see security package) - Spring Security's
 * UserDetailsService/AuthenticationManager flow is never used, and leaving it
 * enabled just auto-creates an unused in-memory user and logs a generated
 * password on every startup.
 *
 * <p>EnableScheduling powers SubscriptionScheduledJob's daily @Scheduled run
 * (invoice generation, grace-period sweep) - no other background job exists
 * in this codebase yet.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class MsmeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsmeApplication.class, args);
    }
}
