package com.xerxes.msme.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * bucketName has no default on purpose - same reasoning as DB_PASSWORD/RAZORPAY_KEY_SECRET.
 * credentialsJson is optional and empty by default - see GcsConfig for how it's used (or not).
 */
@ConfigurationProperties(prefix = "app.gcs")
public record GcsProperties(String bucketName, String credentialsJson) {}
