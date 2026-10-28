package com.xerxes.msme.storefront;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.BusinessRuleViolationException;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.storefront.dto.StorefrontPublishResponse;
import com.xerxes.msme.storefront.dto.StorefrontSettingsRequest;
import com.xerxes.msme.storefront.dto.StorefrontSettingsResponse;
import com.xerxes.msme.storefront.template.StorefrontTemplateRegistry;
import com.xerxes.msme.user.Role;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StorefrontSiteController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorefrontSiteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StorefrontTemplateRegistry templateRegistry;
    @MockBean private StorefrontSettingsService settingsService;
    @MockBean private StorefrontSiteGenerator siteGenerator;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current()).thenReturn(new AuthenticatedPrincipal(userId, tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void templates_returnsRegisteredTemplates() throws Exception {
        when(templateRegistry.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/storefront/templates")).andExpect(status().isOk());
    }

    @Test
    void getSettings_returnsEffectiveSettingsForCurrentTenant() throws Exception {
        StorefrontSettingsResponse response =
                new StorefrontSettingsResponse("veera", Map.of("hero.tagline", "Hello"), null, null);
        when(settingsService.getSettings(tenantId)).thenReturn(response);

        mockMvc.perform(get("/api/storefront/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("veera"));
    }

    @Test
    void updateSettings_savesAndReturnsUpdatedSettings() throws Exception {
        StorefrontSettingsRequest request = new StorefrontSettingsRequest("veera", Map.of("about.heading", "Who we are"));
        StorefrontSettingsResponse response =
                new StorefrontSettingsResponse("veera", Map.of("about.heading", "Who we are"), null, null);
        when(settingsService.updateSettings(eq(tenantId), any())).thenReturn(response);

        mockMvc.perform(put("/api/storefront/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content['about.heading']").value("Who we are"));
    }

    @Test
    void updateSettings_blankTemplateId_returns400() throws Exception {
        StorefrontSettingsRequest request = new StorefrontSettingsRequest("", Map.of());

        mockMvc.perform(put("/api/storefront/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publish_triggersGeneratorAndReturnsPreviewUrl() throws Exception {
        StorefrontPublishResponse response =
                new StorefrontPublishResponse(OffsetDateTime.now(), "/media/" + tenantId + "/static/index.html");
        when(siteGenerator.publish(tenantId)).thenReturn(response);

        mockMvc.perform(post("/api/storefront/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewUrl").value("/media/" + tenantId + "/static/index.html"));
    }

    @Test
    void download_returnsZipWithAttachmentHeader() throws Exception {
        when(siteGenerator.downloadZip(tenantId))
                .thenReturn(new StorefrontSiteGenerator.StorefrontZip("veera-collections-storefront.zip", "zip-bytes".getBytes()));

        mockMvc.perform(get("/api/storefront/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"veera-collections-storefront.zip\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void download_nothingPublishedYet_returns422() throws Exception {
        when(siteGenerator.downloadZip(tenantId))
                .thenThrow(new BusinessRuleViolationException("Nothing has been published yet."));

        mockMvc.perform(get("/api/storefront/download")).andExpect(status().isUnprocessableEntity());
    }
}
