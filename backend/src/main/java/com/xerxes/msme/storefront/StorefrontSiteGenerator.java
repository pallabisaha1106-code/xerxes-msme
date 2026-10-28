package com.xerxes.msme.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.storage.ImageStorageService;
import com.xerxes.msme.storage.TenantStorageService;
import com.xerxes.msme.storefront.dto.PublicVariantResponse;
import com.xerxes.msme.storefront.dto.StorefrontPublishResponse;
import com.xerxes.msme.storefront.template.StorefrontTemplateRegistry;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a tenant's chosen template into a self-contained static site under
 * {@code {tenantId}/static/} (see TenantStorageService) - the "publish"
 * action. A separate download step zips whatever is currently published;
 * see downloadZip. The generated site's own product data is a JSON snapshot
 * taken at publish time (not fetched live once deployed), so stock/price
 * changes need a republish to show up on the vendor's live site - the
 * tradeoff accepted for a genuinely static, offline-capable export.
 */
@Service
public class StorefrontSiteGenerator {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("html", "css", "js", "json", "svg", "txt");

    private final StorefrontTemplateRegistry templateRegistry;
    private final StorefrontSettingsService settingsService;
    private final StorefrontSettingsRepository settingsRepository;
    private final StorefrontCatalogService catalogService;
    private final TenantRepository tenantRepository;
    private final TenantStorageService tenantStorageService;
    private final ImageStorageService imageStorageService;
    private final StorefrontProperties storefrontProperties;
    private final ObjectMapper objectMapper;

    public StorefrontSiteGenerator(
            StorefrontTemplateRegistry templateRegistry,
            StorefrontSettingsService settingsService,
            StorefrontSettingsRepository settingsRepository,
            StorefrontCatalogService catalogService,
            TenantRepository tenantRepository,
            TenantStorageService tenantStorageService,
            ImageStorageService imageStorageService,
            StorefrontProperties storefrontProperties,
            ObjectMapper objectMapper) {
        this.templateRegistry = templateRegistry;
        this.settingsService = settingsService;
        this.settingsRepository = settingsRepository;
        this.catalogService = catalogService;
        this.tenantRepository = tenantRepository;
        this.tenantStorageService = tenantStorageService;
        this.imageStorageService = imageStorageService;
        this.storefrontProperties = storefrontProperties;
        this.objectMapper = objectMapper;
    }

    public record StorefrontZip(String filename, byte[] content) {}

    @Transactional
    public StorefrontPublishResponse publish(UUID tenantId) {
        StorefrontSettings settings = settingsRepository.findByTenantId(tenantId).orElseGet(() -> {
            StorefrontSettings created = new StorefrontSettings();
            created.setTenantId(tenantId);
            return created;
        });
        String templateId = settings.getTemplateId();
        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated request references a missing tenant"));

        Map<String, String> values =
                new LinkedHashMap<>(settingsService.effectiveContent(tenantId, templateId, settings.getContent()));
        values.put("site.businessName", tenant.getBusinessName());
        values.put("site.year", String.valueOf(Year.now().getValue()));
        values.put("site.tenantId", tenantId.toString());
        values.put("site.apiBaseUrl", storefrontProperties.publicApiBaseUrl());
        String whatsappNumber = values.getOrDefault("contact.whatsappNumber", "");
        values.put("contact.whatsappLink", whatsappNumber.isBlank() ? "" : "https://wa.me/" + whatsappNumber);
        values.put("contact.phoneLink", values.getOrDefault("contact.phone", "").replaceAll("[^0-9+]", ""));

        Path outputDir = tenantStorageService.staticDir(tenantId);
        tenantStorageService.recreateEmptyDirectory(outputDir);

        for (Map.Entry<String, byte[]> file : templateRegistry.loadSiteFiles(templateId).entrySet()) {
            writeTemplateFile(outputDir.resolve(file.getKey()), file.getValue(), values);
        }

        List<PublicVariantResponse> products = catalogService.listAvailableProducts(tenantId);
        writeProductsSnapshot(outputDir, products);
        for (PublicVariantResponse product : products) {
            if (!product.imageUrls().isEmpty()) {
                copyVariantImagesFromGcs(
                        tenantId, product.variantId(), outputDir.resolve("images").resolve(product.variantId().toString()));
            }
        }

        OffsetDateTime publishedAt = OffsetDateTime.now();
        settings.setPublishedAt(publishedAt);
        settingsRepository.save(settings);

        return new StorefrontPublishResponse(publishedAt, StorefrontSettingsService.previewUrl(tenantId));
    }

    @Transactional(readOnly = true)
    public StorefrontZip downloadZip(UUID tenantId) {
        StorefrontSettings settings = settingsRepository
                .findByTenantId(tenantId)
                .filter(s -> s.getPublishedAt() != null)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Nothing has been published yet. Generate the site before downloading."));
        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated request references a missing tenant"));

        Path staticDir = tenantStorageService.staticDir(tenantId);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            try (Stream<Path> paths = Files.walk(staticDir)) {
                for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                    String entryName = staticDir.relativize(file).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build storefront zip for tenant " + tenantId, e);
        }

        String filename = slug(tenant.getBusinessName()) + "-storefront.zip";
        return new StorefrontZip(filename, buffer.toByteArray());
    }

    private void writeTemplateFile(Path destination, byte[] content, Map<String, String> values) {
        try {
            Files.createDirectories(destination.getParent());
            String extension = extensionOf(destination);
            if (TEXT_EXTENSIONS.contains(extension)) {
                String text = renderTokens(new String(content, StandardCharsets.UTF_8), values);
                Files.writeString(destination, text, StandardCharsets.UTF_8);
            } else {
                Files.write(destination, content);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + destination, e);
        }
    }

    private void writeProductsSnapshot(Path outputDir, List<PublicVariantResponse> products) {
        List<PublicVariantResponse> localized = products.stream().map(this::localizeImageUrls).toList();
        try {
            Path dataDir = outputDir.resolve("data");
            Files.createDirectories(dataDir);
            objectMapper.writeValue(dataDir.resolve("products.json").toFile(), localized);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write products.json", e);
        }
    }

    /** Pulls a variant's photos out of GCS (see ImageStorageService) and writes them into the local export tree being built. */
    private void copyVariantImagesFromGcs(UUID tenantId, UUID variantId, Path destinationDir) {
        String prefix = tenantId + "/products/" + variantId + "/";
        List<String> objectNames = imageStorageService.listObjectNames(prefix);
        if (objectNames.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(destinationDir);
            for (String objectName : objectNames) {
                byte[] content = imageStorageService.download(objectName);
                String fileName = objectName.substring(objectName.lastIndexOf('/') + 1);
                Files.write(destinationDir.resolve(fileName), content);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy images from GCS for variant " + variantId, e);
        }
    }

    /** The live public API serves images from GCS; the exported zip bundles the same files under images/. */
    private PublicVariantResponse localizeImageUrls(PublicVariantResponse product) {
        return new PublicVariantResponse(
                product.productId(), product.productName(), product.productDescription(), product.variantId(),
                product.sku(), product.attributes(), product.sellingPrice(), localize(product, product.imageUrls()),
                product.inStock(), localize(product, product.thumbnailUrls()), product.sampleImage(),
                localize(product, product.zoomUrls()), product.categoryId(), product.categoryName());
    }

    private static List<String> localize(PublicVariantResponse product, List<String> urls) {
        return urls.stream()
                .map(url -> url == null ? null : "images/" + product.variantId() + "/" + url.substring(url.lastIndexOf('/') + 1))
                .toList();
    }

    static String renderTokens(String text, Map<String, String> values) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static String slug(String value) {
        String slug = value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "storefront" : slug;
    }
}
