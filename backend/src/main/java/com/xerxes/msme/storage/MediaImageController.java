package com.xerxes.msme.storage;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Streams product/marketplace/KYC images out of the (private) GCS bucket -
 * see ImageStorageService.mediaUrl(). Unauthenticated like the /media/**
 * static resource handler in WebConfig (a plain <img>/<iframe> can't attach
 * a Bearer token either way), permitAll'd by the same "/media/**" pattern in
 * SecurityConfig, so no security config change was needed for this.
 */
@RestController
public class MediaImageController {

    private final ImageStorageService imageStorageService;

    public MediaImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/media/images/{*objectPath}")
    public ResponseEntity<byte[]> get(@PathVariable String objectPath) {
        String path = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
        // Vendors' untouched originals live in the same bucket but must never be served: they can carry EXIF (e.g. GPS).
        if (path.contains("/product-originals/")) {
            return ResponseEntity.notFound().build();
        }
        ImageStorageService.StoredImage image = imageStorageService.downloadWithContentType(path);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType =
                image.contentType() != null ? MediaType.parseMediaType(image.contentType()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(image.content());
    }
}
