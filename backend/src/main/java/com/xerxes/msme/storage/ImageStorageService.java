package com.xerxes.msme.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Uploaded product/marketplace photos live in a GCS bucket, not local disk -
 * Cloud Run's own disk is ephemeral and doesn't survive a redeploy or
 * scale-to-zero. Object paths use the same "{tenantId}/products/{variantId}/{fileName}"
 * / "{tenantId}/marketplace/{fileName}" convention the old local folder tree
 * used, so callers built around tenantId/variantId/fileName carry over
 * unchanged - only the storage medium changed. Auth is Application Default
 * Credentials (see application.yml's app.gcs comment) - no key file handled
 * by this class.
 *
 * The bucket itself is deliberately private (no public IAM, no per-object
 * ACLs - those don't even work under Uniform Bucket-Level Access): every
 * image is served back out through MediaImageController's app-side proxy
 * instead, so nothing about the storage layer is exposed directly. See
 * mediaUrl() below.
 */
@Service
public class ImageStorageService {

    private final Storage storage;
    private final String bucketName;

    public ImageStorageService(Storage storage, GcsProperties gcsProperties) {
        this.storage = storage;
        this.bucketName = gcsProperties.bucketName();
    }

    /** Returns the same objectPath - the DB keeps storing bare filenames, unchanged from today. The blob itself stays private; see mediaUrl(). */
    public String upload(String objectPath, byte[] content, String contentType) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, content);
        return objectPath;
    }

    /** Best-effort, like the local-disk deleteQuietly it replaces - an already-missing object isn't an error. */
    public void delete(String objectPath) {
        storage.delete(BlobId.of(bucketName, objectPath));
    }

    public byte[] download(String objectPath) {
        Blob blob = storage.get(BlobId.of(bucketName, objectPath));
        return blob == null ? null : blob.getContent();
    }

    /** Content type travels with the bytes so MediaImageController can set the response header correctly. */
    public record StoredImage(byte[] content, String contentType) {}

    public StoredImage downloadWithContentType(String objectPath) {
        Blob blob = storage.get(BlobId.of(bucketName, objectPath));
        return blob == null ? null : new StoredImage(blob.getContent(), blob.getContentType());
    }

    /** Deletes every object whose path starts with prefix - e.g. a whole variant's image folder. */
    public void deleteAllUnderPrefix(String prefix) {
        List<BlobId> ids = new ArrayList<>();
        for (Blob blob : storage.list(bucketName, Storage.BlobListOption.prefix(prefix)).iterateAll()) {
            ids.add(blob.getBlobId());
        }
        if (!ids.isEmpty()) {
            storage.delete(ids);
        }
    }

    /** Every object's full path under prefix - e.g. for bundling a variant's photos into the storefront export. */
    public List<String> listObjectNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (Blob blob : storage.list(bucketName, Storage.BlobListOption.prefix(prefix)).iterateAll()) {
            names.add(blob.getName());
        }
        return names;
    }

    /** App-relative URL, resolved to an absolute one by the frontends' resolveImagePath() helper - not a direct GCS URL, since the bucket is private. Served by MediaImageController. */
    public String mediaUrl(String objectPath) {
        return "/media/images/" + objectPath;
    }
}
