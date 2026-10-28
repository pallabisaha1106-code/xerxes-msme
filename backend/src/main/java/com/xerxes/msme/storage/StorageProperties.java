package com.xerxes.msme.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Root directory under which every tenant gets its own folder (see {@link TenantStorageService}). */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String root) {}
