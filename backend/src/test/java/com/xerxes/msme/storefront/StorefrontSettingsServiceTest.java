package com.xerxes.msme.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.xerxes.msme.storefront.dto.StorefrontSettingsRequest;
import com.xerxes.msme.storefront.dto.StorefrontSettingsResponse;
import com.xerxes.msme.storefront.template.StorefrontTemplateRegistry;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorefrontSettingsServiceTest {

    @Mock private StorefrontSettingsRepository settingsRepository;
    @Mock private TenantRepository tenantRepository;

    private StorefrontTemplateRegistry templateRegistry;
    private StorefrontSettingsService settingsService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        templateRegistry = new StorefrontTemplateRegistry(new com.fasterxml.jackson.databind.ObjectMapper());
        templateRegistry.loadManifests();
        settingsService = new StorefrontSettingsService(settingsRepository, tenantRepository, templateRegistry);
        tenantId = UUID.randomUUID();

        lenient().when(settingsRepository.save(any(StorefrontSettings.class))).thenAnswer(inv -> {
            StorefrontSettings s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
    }

    private Tenant tenantWith(String businessName, String phone, String email, String addressLine1, String city) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setBusinessName(businessName);
        tenant.setPhone(phone);
        tenant.setEmail(email);
        tenant.setAddressLine1(addressLine1);
        tenant.setCity(city);
        tenant.setStateCode("27");
        return tenant;
    }

    @Test
    void getSettings_noSavedRow_returnsManifestAndTenantDerivedDefaults() {
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenantWith("Veera Collections", "9876543210", "shop@veera.test", "Line 1", "Pune")));

        StorefrontSettingsResponse response = settingsService.getSettings(tenantId);

        assertThat(response.templateId()).isEqualTo("veera");
        assertThat(response.content().get("hero.headline")).isEqualTo("Veera Collections");
        assertThat(response.content().get("contact.phone")).isEqualTo("9876543210");
        assertThat(response.content().get("contact.email")).isEqualTo("shop@veera.test");
        assertThat(response.content().get("contact.address")).contains("Line 1", "Pune");
        assertThat(response.publishedAt()).isNull();
        assertThat(response.previewUrl()).isNull();
    }

    @Test
    void getSettings_savedOverridesWinOverTenantDerivedDefaults() {
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenantWith("Veera Collections", "9876543210", "shop@veera.test", "Line 1", "Pune")));
        StorefrontSettings saved = new StorefrontSettings();
        saved.setId(UUID.randomUUID());
        saved.setTenantId(tenantId);
        saved.setTemplateId("veera");
        saved.setContent(Map.of("hero.headline", "Custom Headline", "contact.phone", "1111111111"));
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(saved));

        StorefrontSettingsResponse response = settingsService.getSettings(tenantId);

        assertThat(response.content().get("hero.headline")).isEqualTo("Custom Headline");
        assertThat(response.content().get("contact.phone")).isEqualTo("1111111111");
        assertThat(response.content().get("contact.email")).isEqualTo("shop@veera.test");
    }

    @Test
    void getSettings_publishedSettings_includesPreviewUrl() {
        when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(tenantWith("Veera Collections", null, null, null, null)));
        StorefrontSettings saved = new StorefrontSettings();
        saved.setId(UUID.randomUUID());
        saved.setTenantId(tenantId);
        saved.setPublishedAt(java.time.OffsetDateTime.now());
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(saved));

        StorefrontSettingsResponse response = settingsService.getSettings(tenantId);

        assertThat(response.publishedAt()).isNotNull();
        assertThat(response.previewUrl()).isEqualTo("/media/" + tenantId + "/static/index.html");
    }

    @Test
    void updateSettings_normalizesWhatsappNumberToDigitsOnly() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWith("Veera Collections", null, null, null, null)));
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        StorefrontSettingsRequest request =
                new StorefrontSettingsRequest("veera", Map.of("contact.whatsappNumber", "+91 98765-43210"));

        settingsService.updateSettings(tenantId, request);

        ArgumentCaptor<StorefrontSettings> captor = ArgumentCaptor.forClass(StorefrontSettings.class);
        org.mockito.Mockito.verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getContent().get("contact.whatsappNumber")).isEqualTo("919876543210");
    }

    @Test
    void updateSettings_existingRow_updatesInPlaceRatherThanCreatingANewOne() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenantWith("Veera Collections", null, null, null, null)));
        StorefrontSettings existing = new StorefrontSettings();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(tenantId);
        when(settingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));

        settingsService.updateSettings(tenantId, new StorefrontSettingsRequest("veera", Map.of("about.heading", "Who we are")));

        ArgumentCaptor<StorefrontSettings> captor = ArgumentCaptor.forClass(StorefrontSettings.class);
        org.mockito.Mockito.verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
        assertThat(captor.getValue().getContent().get("about.heading")).isEqualTo("Who we are");
    }

    @Test
    void updateSettings_unknownTemplateId_throwsResourceNotFound() {
        StorefrontSettingsRequest request = new StorefrontSettingsRequest("not-a-template", Map.of());

        assertThrows(
                com.xerxes.msme.common.exception.ResourceNotFoundException.class,
                () -> settingsService.updateSettings(tenantId, request));
    }
}
