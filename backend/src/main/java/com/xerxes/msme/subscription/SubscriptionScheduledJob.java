package com.xerxes.msme.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily: bill tenants whose period ended, then lock out anyone whose grace period since ran out. */
@Component
public class SubscriptionScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduledJob.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionScheduledJob(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        log.info("Running subscription billing cycle");
        // Each step runs even if the other failed - billing and grace-period lockout are
        // independent concerns, and a scheduled job that throws is otherwise a silent failure
        // (no caller to show an error to, nothing but the framework's own generic log line).
        runStep("generateDueInvoices", subscriptionService::generateDueInvoices);
        runStep("sweepExpiredGracePeriods", subscriptionService::sweepExpiredGracePeriods);
        log.info("Subscription billing cycle complete");
    }

    private void runStep(String stepName, Runnable step) {
        try {
            step.run();
        } catch (Exception ex) {
            log.error("Subscription billing step '{}' failed and needs manual attention", stepName, ex);
        }
    }
}
