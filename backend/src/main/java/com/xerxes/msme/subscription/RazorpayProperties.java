package com.xerxes.msme.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** No defaults on purpose - must come from the environment, same as DB_PASSWORD/JWT_SECRET. */
@ConfigurationProperties(prefix = "app.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret) {}
