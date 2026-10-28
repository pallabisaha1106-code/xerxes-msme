package com.xerxes.msme.subscription.dto;

import com.xerxes.msme.subscription.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Super Admin manual override - mark an offline payment received, comp/extend a tenant, or force-suspend. */
public record AdminSubscriptionUpdateRequest(@NotNull SubscriptionStatus status, OffsetDateTime currentPeriodEnd) {}
