package com.xerxes.msme.subscription;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class SubscriptionScheduledJobTest {

    @Test
    void run_bothStepsSucceed_runsBoth() {
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        SubscriptionScheduledJob job = new SubscriptionScheduledJob(subscriptionService);

        job.run();

        verify(subscriptionService).generateDueInvoices();
        verify(subscriptionService).sweepExpiredGracePeriods();
    }

    @Test
    void run_generateDueInvoicesThrows_sweepExpiredGracePeriodsStillRuns() {
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        doThrow(new RuntimeException("billing exploded")).when(subscriptionService).generateDueInvoices();
        SubscriptionScheduledJob job = new SubscriptionScheduledJob(subscriptionService);

        job.run();

        verify(subscriptionService).sweepExpiredGracePeriods();
    }

    @Test
    void run_sweepExpiredGracePeriodsThrows_doesNotPreventGenerateDueInvoicesFromHavingRun() {
        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        doThrow(new RuntimeException("sweep exploded")).when(subscriptionService).sweepExpiredGracePeriods();
        SubscriptionScheduledJob job = new SubscriptionScheduledJob(subscriptionService);

        job.run();

        verify(subscriptionService).generateDueInvoices();
    }
}
