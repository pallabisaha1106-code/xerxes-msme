package com.xerxes.msme.storefront;

import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.storefront.dto.StorefrontPublishResponse;
import com.xerxes.msme.storefront.dto.StorefrontSettingsRequest;
import com.xerxes.msme.storefront.dto.StorefrontSettingsResponse;
import com.xerxes.msme.storefront.template.StorefrontTemplateRegistry;
import com.xerxes.msme.storefront.template.TemplateManifest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated admin endpoints for building a tenant's storefront site - template choice, content, publish, download. */
@RestController
@RequestMapping("/api/storefront")
public class StorefrontSiteController {

    private final StorefrontTemplateRegistry templateRegistry;
    private final StorefrontSettingsService settingsService;
    private final StorefrontSiteGenerator siteGenerator;
    private final AuthContext authContext;

    public StorefrontSiteController(
            StorefrontTemplateRegistry templateRegistry,
            StorefrontSettingsService settingsService,
            StorefrontSiteGenerator siteGenerator,
            AuthContext authContext) {
        this.templateRegistry = templateRegistry;
        this.settingsService = settingsService;
        this.siteGenerator = siteGenerator;
        this.authContext = authContext;
    }

    @GetMapping("/templates")
    public List<TemplateManifest> templates() {
        return templateRegistry.findAll();
    }

    @GetMapping("/settings")
    public StorefrontSettingsResponse getSettings() {
        return settingsService.getSettings(authContext.current().tenantId());
    }

    @PutMapping("/settings")
    public StorefrontSettingsResponse updateSettings(@Valid @RequestBody StorefrontSettingsRequest request) {
        return settingsService.updateSettings(authContext.current().tenantId(), request);
    }

    @PostMapping("/publish")
    public StorefrontPublishResponse publish() {
        return siteGenerator.publish(authContext.current().tenantId());
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download() {
        StorefrontSiteGenerator.StorefrontZip zip = siteGenerator.downloadZip(authContext.current().tenantId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zip.filename() + "\"")
                .body(zip.content());
    }
}
