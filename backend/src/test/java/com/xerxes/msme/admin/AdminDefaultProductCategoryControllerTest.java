package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminDefaultProductCategoryResponse;
import com.xerxes.msme.admin.dto.CreateDefaultProductCategoryRequest;
import com.xerxes.msme.admin.dto.UpdateDefaultProductCategoryRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminDefaultProductCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDefaultProductCategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminDefaultProductCatalogService catalogService;

    private final UUID marketplaceCategoryId = UUID.randomUUID();

    @Test
    void list_returnsCategoriesForMarketplaceCategory() throws Exception {
        when(catalogService.listCategories(marketplaceCategoryId))
                .thenReturn(List.of(new AdminDefaultProductCategoryResponse(UUID.randomUUID(), marketplaceCategoryId, "Rice", 1, true)));

        mockMvc.perform(get("/api/admin/default-product-categories").param("marketplaceCategoryId", marketplaceCategoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rice"));
    }

    @Test
    void create_delegatesToService() throws Exception {
        CreateDefaultProductCategoryRequest request = new CreateDefaultProductCategoryRequest(marketplaceCategoryId, "Rice", 1);
        when(catalogService.createCategory(eq(request)))
                .thenReturn(new AdminDefaultProductCategoryResponse(UUID.randomUUID(), marketplaceCategoryId, "Rice", 1, true));

        mockMvc.perform(post("/api/admin/default-product-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rice"));
    }

    @Test
    void update_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateDefaultProductCategoryRequest request = new UpdateDefaultProductCategoryRequest("Rice & Grains", 2, false);
        when(catalogService.updateCategory(eq(id), any()))
                .thenReturn(new AdminDefaultProductCategoryResponse(id, marketplaceCategoryId, "Rice & Grains", 2, false));

        mockMvc.perform(put("/api/admin/default-product-categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
