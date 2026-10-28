package com.xerxes.msme.storefront;

import com.xerxes.msme.storefront.dto.StorefrontSettingsRequest;
import com.xerxes.msme.storefront.dto.StorefrontSettingsResponse;
import com.xerxes.msme.storefront.template.StorefrontTemplateRegistry;
import com.xerxes.msme.storefront.template.TemplateManifest;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the "effective content" merge for a tenant's storefront: the
 * template's manifest defaults, topped up with a few computed defaults
 * pulled from the tenant's own registration details (so a shop doesn't have
 * to retype its phone/email/address), topped up again with whatever the
 * tenant has explicitly saved. StorefrontSiteGenerator reads this same merge
 * when it renders the site, so the admin form and the published output never
 * disagree about what a field's current value is.
 */
@Service
public class StorefrontSettingsService {

    private final StorefrontSettingsRepository settingsRepository;
    private final TenantRepository tenantRepository;
    private final StorefrontTemplateRegistry templateRegistry;

    public StorefrontSettingsService(
            StorefrontSettingsRepository settingsRepository,
            TenantRepository tenantRepository,
            StorefrontTemplateRegistry templateRegistry) {
        this.settingsRepository = settingsRepository;
        this.tenantRepository = tenantRepository;
        this.templateRegistry = templateRegistry;
    }

    @Transactional(readOnly = true)
    public StorefrontSettingsResponse getSettings(UUID tenantId) {
        StorefrontSettings settings = settingsRepository.findByTenantId(tenantId).orElse(null);
        String templateId = settings != null ? settings.getTemplateId() : "veera";
        Map<String, String> overrides = settings != null ? settings.getContent() : Map.of();
        Map<String, String> content = effectiveContent(tenantId, templateId, overrides);
        OffsetDateTime publishedAt = settings != null ? settings.getPublishedAt() : null;
        String previewUrl = publishedAt != null ? previewUrl(tenantId) : null;
        return new StorefrontSettingsResponse(templateId, content, publishedAt, previewUrl);
    }

    @Transactional
    public StorefrontSettingsResponse updateSettings(UUID tenantId, StorefrontSettingsRequest request) {
        templateRegistry.find(request.templateId());
        StorefrontSettings settings = settingsRepository.findByTenantId(tenantId).orElseGet(() -> {
            StorefrontSettings created = new StorefrontSettings();
            created.setTenantId(tenantId);
            return created;
        });
        settings.setTemplateId(request.templateId());
        settings.setContent(normalize(request.content()));
        settingsRepository.save(settings);
        return getSettings(tenantId);
    }

    /** Merge order: manifest defaults, then tenant-derived defaults, then explicit tenant overrides. */
    Map<String, String> effectiveContent(UUID tenantId, String templateId, Map<String, String> overrides) {
        TemplateManifest manifest = templateRegistry.find(templateId);
        Map<String, String> merged = new LinkedHashMap<>(manifest.defaultValues());

        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated request references a missing tenant"));
        if (merged.containsKey("hero.headline")) {
            merged.put("hero.headline", tenant.getBusinessName());
        }
        if (merged.containsKey("contact.phone")) {
            merged.put("contact.phone", nullToEmpty(tenant.getPhone()));
        }
        if (merged.containsKey("contact.email")) {
            merged.put("contact.email", nullToEmpty(tenant.getEmail()));
        }
        if (merged.containsKey("contact.address")) {
            merged.put("contact.address", composeAddress(tenant));
        }

        merged.putAll(overrides);
        return merged;
    }

    /** WhatsApp deep links and the JS this gets embedded into both require digits only. */
    private static Map<String, String> normalize(Map<String, String> content) {
        Map<String, String> normalized = new LinkedHashMap<>(content);
        if (normalized.containsKey("contact.whatsappNumber")) {
            normalized.put("contact.whatsappNumber", normalized.get("contact.whatsappNumber").replaceAll("[^0-9]", ""));
        }
        return normalized;
    }

    private static String composeAddress(Tenant tenant) {
        return Stream.of(tenant.getAddressLine1(), tenant.getAddressLine2(), tenant.getCity(), tenant.getPincode())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String previewUrl(UUID tenantId) {
        return "/media/" + tenantId + "/static/index.html";
    }
}
