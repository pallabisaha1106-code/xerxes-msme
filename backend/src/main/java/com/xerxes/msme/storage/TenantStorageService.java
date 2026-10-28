package com.xerxes.msme.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Local-disk working directory for the generated storefront static-site
 * export only ({@code {root}/{tenantId}/static/}) - see StorefrontSiteGenerator.
 * Product/marketplace photos are NOT here; they live in GCS (see
 * ImageStorageService), since Cloud Run's local disk doesn't survive a
 * redeploy or scale-to-zero and photos need to be permanent. The static
 * export tree is regenerated on every publish, so losing it between
 * publishes is an accepted, much lower-stakes tradeoff.
 */
@Service
public class TenantStorageService {

    private final Path storageRoot;

    public TenantStorageService(StorageProperties storageProperties) {
        this.storageRoot = Path.of(storageProperties.root()).toAbsolutePath().normalize();
    }

    public Path tenantRoot(UUID tenantId) {
        return storageRoot.resolve(tenantId.toString());
    }

    /** Reserved for the generated storefront export - see StorefrontSiteGenerator. */
    public Path staticDir(UUID tenantId) {
        return tenantRoot(tenantId).resolve("static");
    }

    /** Recreates a directory empty, deleting anything already there. Used before each fresh publish. */
    public void recreateEmptyDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> paths = Files.walk(dir)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Could not delete " + path, e);
                        }
                    });
                }
            }
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not recreate directory " + dir, e);
        }
    }
}
