package com.xerxes.msme.subscription;

import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.subscription.dto.SubscriptionStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A tenant's own billing status - reachable even when SUSPENDED (see WebConfig's interceptor exclusion). */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthContext authContext;

    public SubscriptionController(SubscriptionService subscriptionService, AuthContext authContext) {
        this.subscriptionService = subscriptionService;
        this.authContext = authContext;
    }

    @GetMapping
    public SubscriptionStatusResponse getStatus() {
        return subscriptionService.getStatus(authContext.current().tenantId());
    }
}
