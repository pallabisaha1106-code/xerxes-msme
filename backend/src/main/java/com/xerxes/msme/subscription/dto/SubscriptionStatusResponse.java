package com.xerxes.msme.subscription.dto;

import com.xerxes.msme.subscription.SubscriptionStatus;
import java.time.OffsetDateTime;

public record SubscriptionStatusResponse(
        SubscriptionStatus status,
        OffsetDateTime trialEndsAt,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime gracePeriodEndsAt,
        PendingInvoiceResponse pendingInvoice) {}
