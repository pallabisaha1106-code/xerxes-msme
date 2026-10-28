package com.xerxes.msme.storefront.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorefrontTemplateRegistryTest {

    private StorefrontTemplateRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StorefrontTemplateRegistry(new ObjectMapper());
        registry.loadManifests();
    }

    @Test
    void findAll_includesTheVeeraTemplate() {
        assertThat(registry.findAll()).extracting(TemplateManifest::id).contains("veera");
    }

    @Test
    void find_knownTemplate_returnsManifestWithSectionsAndFields() {
        TemplateManifest manifest = registry.find("veera");

        assertThat(manifest.name()).isNotBlank();
        assertThat(manifest.sections()).isNotEmpty();
        assertThat(manifest.defaultValues()).containsKey("hero.tagline");
    }

    @Test
    void find_unknownTemplate_throwsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> registry.find("does-not-exist"));
    }

    @Test
    void loadSiteFiles_returnsIndexHtmlAndAssetsWithContent() {
        Map<String, byte[]> files = registry.loadSiteFiles("veera");

        assertThat(files).containsKey("index.html");
        assertThat(files).containsKey("css/site.css");
        assertThat(files).containsKey("js/catalog.js");
        String indexHtml = new String(files.get("index.html"), StandardCharsets.UTF_8);
        assertThat(indexHtml).contains("{{site.businessName}}");
    }
}
