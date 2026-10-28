package com.xerxes.msme.storefront.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Templates are classpath resource bundles under storefront-templates/{id}/,
 * each with a manifest.json (see TemplateManifest) and a site/ folder of raw
 * HTML/CSS/JS that StorefrontSiteGenerator token-replaces at publish time.
 * Adding a new template is dropping in a new folder here, not writing code -
 * this registry just discovers whatever manifests exist on the classpath.
 */
@Component
public class StorefrontTemplateRegistry {

    private static final String BASE_PATH = "classpath:storefront-templates/";

    private final ObjectMapper objectMapper;
    private final Map<String, TemplateManifest> manifestsById = new LinkedHashMap<>();

    public StorefrontTemplateRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadManifests() {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(BASE_PATH + "*/manifest.json");
            for (Resource resource : resources) {
                try (var in = resource.getInputStream()) {
                    TemplateManifest manifest = objectMapper.readValue(in, TemplateManifest.class);
                    manifestsById.put(manifest.id(), manifest);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load storefront templates", e);
        }
    }

    public List<TemplateManifest> findAll() {
        return List.copyOf(manifestsById.values());
    }

    public TemplateManifest find(String templateId) {
        TemplateManifest manifest = manifestsById.get(templateId);
        if (manifest == null) {
            throw ResourceNotFoundException.of("StorefrontTemplate", templateId);
        }
        return manifest;
    }

    /** Every file under this template's site/ folder, keyed by its path relative to that folder. */
    public Map<String, byte[]> loadSiteFiles(String templateId) {
        find(templateId);
        String basePath = BASE_PATH + templateId + "/site/";
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            String baseUrl = resolver.getResource(basePath).getURL().toString();
            Resource[] resources = resolver.getResources(basePath + "**/*.*");
            Map<String, byte[]> files = new LinkedHashMap<>();
            for (Resource resource : resources) {
                String relativePath = resource.getURL().toString().substring(baseUrl.length());
                try (var in = resource.getInputStream()) {
                    files.put(relativePath, StreamUtils.copyToByteArray(in));
                }
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load site files for template " + templateId, e);
        }
    }
}
