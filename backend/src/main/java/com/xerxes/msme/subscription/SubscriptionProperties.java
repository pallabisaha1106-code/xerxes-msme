package com.xerxes.msme.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subscription")
/** Price is per-plan now (see MembershipType.monthlyAmountPaise) - this just holds the trial/grace timings. */
public record SubscriptionProperties(int trialDays, int gracePeriodDays) {}
