package com.xerxes.msme.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    private static final String BUCKET = "test-bucket";

    @Mock private Storage storage;

    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageService(storage, new GcsProperties(BUCKET, null));
    }

    @Test
    void upload_writesTheBlobAndReturnsTheObjectPath() {
        byte[] content = "fake-jpeg-bytes".getBytes();

        String result = imageStorageService.upload("tenant-1/products/variant-1/photo.jpg", content, "image/jpeg");

        assertThat(result).isEqualTo("tenant-1/products/variant-1/photo.jpg");
        verify(storage)
                .create(
                        eq(BlobInfo.newBuilder(BlobId.of(BUCKET, "tenant-1/products/variant-1/photo.jpg"))
                                .setContentType("image/jpeg")
                                .build()),
                        eq(content));
    }

    @Test
    void delete_deletesTheBlobByIdRegardlessOfWhetherItExisted() {
        imageStorageService.delete("tenant-1/marketplace/cover.jpg");

        verify(storage).delete(BlobId.of(BUCKET, "tenant-1/marketplace/cover.jpg"));
    }

    @Test
    void download_existingBlob_returnsItsContent() {
        Blob blob = mockBlobWithContent("hello".getBytes());
        when(storage.get(BlobId.of(BUCKET, "tenant-1/marketplace/cover.jpg"))).thenReturn(blob);

        assertThat(imageStorageService.download("tenant-1/marketplace/cover.jpg")).isEqualTo("hello".getBytes());
    }

    @Test
    void download_missingBlob_returnsNull() {
        when(storage.get(any(BlobId.class))).thenReturn(null);

        assertThat(imageStorageService.download("does/not/exist.jpg")).isNull();
    }

    @Test
    void downloadWithContentType_existingBlob_returnsContentAndContentType() {
        Blob blob = mockBlobWithContent("hello".getBytes());
        when(blob.getContentType()).thenReturn("image/jpeg");
        when(storage.get(BlobId.of(BUCKET, "tenant-1/marketplace/cover.jpg"))).thenReturn(blob);

        ImageStorageService.StoredImage result = imageStorageService.downloadWithContentType("tenant-1/marketplace/cover.jpg");

        assertThat(result.content()).isEqualTo("hello".getBytes());
        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void downloadWithContentType_missingBlob_returnsNull() {
        when(storage.get(any(BlobId.class))).thenReturn(null);

        assertThat(imageStorageService.downloadWithContentType("does/not/exist.jpg")).isNull();
    }

    @Test
    void deleteAllUnderPrefix_deletesEveryListedBlob() {
        BlobId id1 = BlobId.of(BUCKET, "tenant-1/products/variant-1/main.jpg");
        BlobId id2 = BlobId.of(BUCKET, "tenant-1/products/variant-1/main-480.jpg");
        Blob blob1 = mockBlobWithId(id1);
        Blob blob2 = mockBlobWithId(id2);
        Page<Blob> page = mockPage(List.of(blob1, blob2));
        when(storage.list(eq(BUCKET), any(Storage.BlobListOption.class))).thenReturn(page);

        imageStorageService.deleteAllUnderPrefix("tenant-1/products/variant-1/");

        verify(storage).delete(List.of(id1, id2));
    }

    @Test
    void deleteAllUnderPrefix_noMatches_doesNotCallDelete() {
        Page<Blob> page = mockPage(List.of());
        when(storage.list(eq(BUCKET), any(Storage.BlobListOption.class))).thenReturn(page);

        imageStorageService.deleteAllUnderPrefix("nothing/here/");

        verify(storage, never()).delete(any(List.class));
    }

    @Test
    void listObjectNames_returnsEveryBlobNameFromThePage() {
        Blob blob1 = mockBlobWithName("tenant-1/products/variant-1/main.jpg");
        Blob blob2 = mockBlobWithName("tenant-1/products/variant-1/main-480.jpg");
        Page<Blob> page = mockPage(List.of(blob1, blob2));
        when(storage.list(eq(BUCKET), any(Storage.BlobListOption.class))).thenReturn(page);

        assertThat(imageStorageService.listObjectNames("tenant-1/products/variant-1/"))
                .containsExactly("tenant-1/products/variant-1/main.jpg", "tenant-1/products/variant-1/main-480.jpg");
    }

    @Test
    void mediaUrl_buildsTheAppRelativeProxyUrl() {
        assertThat(imageStorageService.mediaUrl("tenant-1/marketplace/cover.jpg"))
                .isEqualTo("/media/images/tenant-1/marketplace/cover.jpg");
    }

    private static Blob mockBlobWithContent(byte[] content) {
        Blob blob = org.mockito.Mockito.mock(Blob.class);
        when(blob.getContent()).thenReturn(content);
        return blob;
    }

    private static Blob mockBlobWithId(BlobId id) {
        Blob blob = org.mockito.Mockito.mock(Blob.class);
        when(blob.getBlobId()).thenReturn(id);
        return blob;
    }

    private static Blob mockBlobWithName(String name) {
        Blob blob = org.mockito.Mockito.mock(Blob.class);
        when(blob.getName()).thenReturn(name);
        return blob;
    }

    @SuppressWarnings("unchecked")
    private static Page<Blob> mockPage(List<Blob> blobs) {
        Page<Blob> page = org.mockito.Mockito.mock(Page.class);
        when(page.iterateAll()).thenReturn(blobs);
        return page;
    }
}
