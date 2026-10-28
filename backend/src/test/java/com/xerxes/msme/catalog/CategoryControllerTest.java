package com.xerxes.msme.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.catalog.dto.CategoryRequest;
import com.xerxes.msme.catalog.dto.CategoryResponse;
import com.xerxes.msme.common.BulkImportError;
import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CategoryService categoryService;
    @MockBean private AuthContext authContext;
    @MockBean private BulkImportSupport bulkImportSupport;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CategoryResponse response = new CategoryResponse(UUID.randomUUID(), "Apparel", null);
        when(categoryService.create(eq(tenantId), any())).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Apparel", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Apparel"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsCategoriesForCurrentTenant() throws Exception {
        when(categoryService.list(tenantId))
                .thenReturn(List.of(new CategoryResponse(UUID.randomUUID(), "Beverages", null)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Beverages"));
    }

    @Test
    void get_returnsCategoryById() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(categoryService.get(tenantId, categoryId))
                .thenReturn(new CategoryResponse(categoryId, "Electronics", null));

        mockMvc.perform(get("/api/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bulkCreate_delegatesToBulkImportSupportWithTenantScopedCreator() throws Exception {
        BulkImportResult result =
                new BulkImportResult(1, 1, List.of(new BulkImportError(1, "Category already exists: Apparel")));
        when(bulkImportSupport.run(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/categories/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                List.of(new CategoryRequest("Footwear", null), new CategoryRequest("Apparel", null)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andExpect(jsonPath("$.errors[0].rowIndex").value(1));

        ArgumentCaptor<List<CategoryRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Consumer<CategoryRequest>> creatorCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(bulkImportSupport).run(requestsCaptor.capture(), creatorCaptor.capture());
        assertThat(requestsCaptor.getValue()).hasSize(2);

        // Exercise the captured creator lambda to prove it's wired to the
        // service with the current tenant, not just structurally present.
        CategoryRequest request = new CategoryRequest("Sportswear", null);
        creatorCaptor.getValue().accept(request);
        verify(categoryService).create(tenantId, request);
    }
}
