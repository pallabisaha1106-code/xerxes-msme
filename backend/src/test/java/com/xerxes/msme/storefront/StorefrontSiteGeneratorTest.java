package com.xerxes.msme.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.storage.StorageProperties;
import com.xerxes.msme.storage.TenantStorageService;
import com.xerxes.msme.storefront.dto.PublicVariantResponse;
import com.xerxes.msme.storefront.dto.StorefrontPublishResponse;
import com.xerxes.msme.storefront.template.StorefrontTemplateRegistry;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorefrontSiteGeneratorTest {

    @TempDir private Path tempStorageRoot;

    @Mock private StorefrontSettingsService settingsService;
    @Mock private StorefrontSettingsRepository settingsRepository;
    @Mock private StorefrontCatalogService catalogService;
    @Mock private TenantRepository tenantRepository;
    @Mock private ImageStorageService imageStorageService;

    private StorefrontTemplateRegistry templateRegistry;
    private TenantStorageService tenantStorageService;
    private StorefrontSiteGenerator generator;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        templateRegistry = new StorefrontTemplateRegistry(new ObjectMapper());
        templateRegistry.loadManifests();
        tenantStorageService = new TenantStorageService(new StorageProperties(tempStorageRoot.toString()));
        generator = new StorefrontSiteGenerator(
                templateRegistry, settingsService, settingsRepository, catalogService, tenantRepository,
                tenantStorageService, imageStorageService, new StorefrontProperties("http://localhost:8080", true),
                new ObjectMapper());
        tenantId = UUID.randomUUID();

        lenient()
                .when(settingsRepository.save(any(StorefrontSettings.class)))
                .thenAnswer(inv -> {
                    StorefrontSettings s = inv.getArgument(0);
                    if (s.getId() == null) s.setId(UUID.randomUUID());
                    return s;
                });
        lenient().when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        lenient().when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        lenient()
                .when(settingsService.effectiveContent(eq(tenantId), anyString(), anyMap()))
                .thenReturn(Map.of(
                        "hero.eyebrow", "Fashion for every occasion",
                        "hero.headline", "Veera Collections",
                        "hero.tagline", "Discover our latest collection",
                        "contact.whatsappNumber", "919876543210",
                        "contact.phone", "9876543210"));
        lenient().when(catalogService.listAvailableProducts(tenantId)).thenReturn(List.of());
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName("Veera Collections");
        tenant.setStateCode("27");
        return tenant;
    }

    private String readOutputFile(String relativePath) throws Exception {
        Path file = tenantStorageService.staticDir(tenantId).resolve(relativePath);
        return Files.readString(file);
    }

    @Test
    void publish_rendersIndexHtmlWithNoLeftoverTokens() throws Exception {
        generator.publish(tenantId);

        String indexHtml = readOutputFile("index.html");
        assertThat(indexHtml).contains("Veera Collections");
        assertThat(indexHtml).contains("Discover our latest collection");
        assertThat(indexHtml).doesNotContain("{{");
    }

    @Test
    void publish_computesWhatsappLinkFromConfiguredNumber() throws Exception {
        generator.publish(tenantId);

        String indexHtml = readOutputFile("index.html");
        assertThat(indexHtml).contains("https://wa.me/919876543210");
    }

    @Test
    void publish_writesConfigJsWithTenantIdAndApiBaseUrl() throws Exception {
        generator.publish(tenantId);

        String configJs = readOutputFile("js/config.js");
        assertThat(configJs).contains("tenantId: \"" + tenantId + "\"");
        assertThat(configJs).contains("apiBaseUrl: \"http://localhost:8080\"");
    }

    @Test
    void publish_writesProductsJsonWithLocalizedImageUrls() throws Exception {
        UUID variantId = UUID.randomUUID();
        PublicVariantResponse product = new PublicVariantResponse(
                UUID.randomUUID(), "Cotton Kurti", "desc", variantId, "KURTI-M", Map.of("size", "M"),
                new BigDecimal("499.00"), List.of("/media/" + tenantId + "/products/" + variantId + "/abc123.jpg"), true);
        when(catalogService.listAvailableProducts(tenantId)).thenReturn(List.of(product));

        generator.publish(tenantId);

        String productsJson = readOutputFile("data/products.json");
        assertThat(productsJson).contains("\"images/" + variantId + "/abc123.jpg\"");
        assertThat(productsJson).doesNotContain("/media/");
    }

    @Test
    void publish_copiesVariantImageFilesFromGcsIntoOutput() throws Exception {
        UUID variantId = UUID.randomUUID();
        String prefix = tenantId + "/products/" + variantId + "/";
        when(imageStorageService.listObjectNames(prefix))
                .thenReturn(List.of(prefix + "abc123.jpg", prefix + "abc123-480.jpg"));
        when(imageStorageService.download(prefix + "abc123.jpg")).thenReturn("fake-jpeg-bytes".getBytes());
        when(imageStorageService.download(prefix + "abc123-480.jpg")).thenReturn("fake-thumbnail-bytes".getBytes());

        PublicVariantResponse product = new PublicVariantResponse(
                UUID.randomUUID(), "Cotton Kurti", "desc", variantId, "KURTI-M", Map.of(),
                new BigDecimal("499.00"), List.of("/media/" + tenantId + "/products/" + variantId + "/abc123.jpg"), true);
        when(catalogService.listAvailableProducts(tenantId)).thenReturn(List.of(product));

        generator.publish(tenantId);

        Path copiedMain = tenantStorageService.staticDir(tenantId).resolve("images/" + variantId + "/abc123.jpg");
        Path copiedThumb = tenantStorageService.staticDir(tenantId).resolve("images/" + variantId + "/abc123-480.jpg");
        assertThat(copiedMain).exists();
        assertThat(copiedThumb).exists();
        assertThat(Files.readString(copiedMain)).isEqualTo("fake-jpeg-bytes");
    }

    @Test
    void publish_setsPublishedAtAndPersistsSettings() {
        StorefrontPublishResponse response = generator.publish(tenantId);

        assertThat(response.publishedAt()).isNotNull();
        assertThat(response.previewUrl()).isEqualTo("/media/" + tenantId + "/static/index.html");
        verify(settingsRepository).save(any(StorefrontSettings.class));
    }

    @Test
    void publish_reRunOverwritesPreviousOutput() throws Exception {
        generator.publish(tenantId);
        Path staleFile = tenantStorageService.staticDir(tenantId).resolve("stale.txt");
        Files.writeString(staleFile, "leftover from a previous template choice");

        generator.publish(tenantId);

        assertThat(staleFile).doesNotExist();
        assertThat(readOutputFile("index.html")).contains("Veera Collections");
    }

    @Test
    void downloadZip_notYetPublished_throwsBusinessRuleViolation() {
        assertThrows(BusinessRuleViolationException.class, () -> generator.downloadZip(tenantId));
    }

    @Test
    void downloadZip_afterPublish_returnsZipContainingIndexHtml() throws Exception {
        StorefrontSettings published = new StorefrontSettings();
        published.setId(UUID.randomUUID());
        published.setTenantId(tenantId);
        published.setPublishedAt(OffsetDateTime.now());
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(published));

        generator.publish(tenantId);
        StorefrontSiteGenerator.StorefrontZip zip = generator.downloadZip(tenantId);

        assertThat(zip.filename()).endsWith("-storefront.zip");
        assertThat(zip.content()).isNotEmpty();

        boolean hasIndexHtml = false;
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zip.content()))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (entry.getName().equals("index.html")) hasIndexHtml = true;
            }
        }
        assertThat(hasIndexHtml).isTrue();
    }

    @Test
    void renderTokens_missingKeyResolvesToEmptyString() {
        String result = StorefrontSiteGenerator.renderTokens("Hello {{site.businessName}}, {{unknown.key}}!", Map.of("site.businessName", "Veera"));

        assertThat(result).isEqualTo("Hello Veera, !");
    }
}
