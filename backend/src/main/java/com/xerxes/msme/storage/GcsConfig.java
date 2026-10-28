package com.xerxes.msme.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** The Storage client is its own bean (rather than built inside ImageStorageService) so tests can inject a fake one. */
@Configuration
public class GcsConfig {

    /**
     * Credentials come from GCS_CREDENTIALS_JSON (the service account key's JSON content, not a
     * file path) when set - lets local dev skip `gcloud auth application-default login` by just
     * pasting a downloaded key into an env var instead. When it's not set, falls back to
     * Application Default Credentials - which is how this works in production, where Cloud Run
     * supplies credentials from its own attached service account automatically, no key needed.
     */
    @Bean
    public Storage storage(GcsProperties gcsProperties) throws IOException {
        String credentialsJson = gcsProperties.credentialsJson();
        if (StringUtils.hasText(credentialsJson)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
            return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        }
        return StorageOptions.getDefaultInstance().getService();
    }
}
