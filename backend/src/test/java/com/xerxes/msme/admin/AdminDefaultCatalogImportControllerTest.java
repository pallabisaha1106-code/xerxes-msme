package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogRequest;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminDefaultCatalogImportController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDefaultCatalogImportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminDefaultProductCatalogService catalogService;

    @Test
    void importCatalog_manifestWithImages_delegatesToServiceAndReturnsSummary() throws Exception {
        UUID marketplaceCategoryId = UUID.randomUUID();
        ImportDefaultCatalogRequest request = new ImportDefaultCatalogRequest(
                marketplaceCategoryId,
                List.of(new ImportDefaultCatalogRequest.Category(
                        "Rice", List.of(new ImportDefaultCatalogRequest.Product("Kohinoor Basmati Rice", "groc-1-1.jpg")))));
        MockMultipartFile manifestPart =
                new MockMultipartFile("manifest", "", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile imagePart = new MockMultipartFile("images", "groc-1-1.jpg", "image/jpeg", "bytes".getBytes());

        when(catalogService.importCatalog(any(), any())).thenReturn(new ImportDefaultCatalogResponse(1, 0, 1, 0, 1, java.util.List.of()));

        mockMvc.perform(multipart("/api/admin/default-catalog/import").file(manifestPart).file(imagePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriesCreated").value(1))
                .andExpect(jsonPath("$.imagesAttached").value(1));
    }

    @Test
    void importCatalog_noImages_stillWorks() throws Exception {
        UUID marketplaceCategoryId = UUID.randomUUID();
        ImportDefaultCatalogRequest request = new ImportDefaultCatalogRequest(
                marketplaceCategoryId,
                List.of(new ImportDefaultCatalogRequest.Category(
                        "Rice", List.of(new ImportDefaultCatalogRequest.Product("Kohinoor Basmati Rice", null)))));
        MockMultipartFile manifestPart =
                new MockMultipartFile("manifest", "", "application/json", objectMapper.writeValueAsBytes(request));

        when(catalogService.importCatalog(any(), any())).thenReturn(new ImportDefaultCatalogResponse(1, 0, 1, 0, 0, java.util.List.of()));

        mockMvc.perform(multipart("/api/admin/default-catalog/import").file(manifestPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagesAttached").value(0));
    }
}
