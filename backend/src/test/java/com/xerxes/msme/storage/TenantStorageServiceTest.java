package com.xerxes.msme.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TenantStorageServiceTest {

    @TempDir private Path tempDir;

    private TenantStorageService tenantStorageService;

    @BeforeEach
    void setUp() {
        tenantStorageService = new TenantStorageService(new StorageProperties(tempDir.toString()));
    }

    @Test
    void staticDir_nestsUnderTenantRoot() {
        UUID tenantId = UUID.randomUUID();

        Path dir = tenantStorageService.staticDir(tenantId);

        assertThat(dir).isEqualTo(tenantStorageService.tenantRoot(tenantId).resolve("static"));
    }

    @Test
    void recreateEmptyDirectory_missingDirectory_createsIt() {
        Path dir = tempDir.resolve("brand-new");

        tenantStorageService.recreateEmptyDirectory(dir);

        assertThat(Files.isDirectory(dir)).isTrue();
    }

    @Test
    void recreateEmptyDirectory_existingDirectoryWithFiles_wipesItEmpty() throws IOException {
        Path dir = tempDir.resolve("existing");
        Files.createDirectories(dir.resolve("nested"));
        Files.writeString(dir.resolve("stale.txt"), "old content");
        Files.writeString(dir.resolve("nested/also-stale.txt"), "old content");

        tenantStorageService.recreateEmptyDirectory(dir);

        assertThat(Files.isDirectory(dir)).isTrue();
        try (var entries = Files.list(dir)) {
            assertThat(entries).isEmpty();
        }
    }
}
