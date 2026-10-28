package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminDefaultProductResponse;
import com.xerxes.msme.admin.dto.CreateDefaultProductRequest;
import com.xerxes.msme.admin.dto.UpdateDefaultProductRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminDefaultProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDefaultProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminDefaultProductCatalogService catalogService;

    private final UUID categoryId = UUID.randomUUID();

    @Test
    void list_returnsProductsForCategory() throws Exception {
        when(catalogService.listProducts(categoryId))
                .thenReturn(
                        List.of(new AdminDefaultProductResponse(UUID.randomUUID(), categoryId, "Kohinoor Basmati Rice", 1, true, null)));

        mockMvc.perform(get("/api/admin/default-products").param("defaultProductCategoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kohinoor Basmati Rice"));
    }

    @Test
    void create_delegatesToService() throws Exception {
        CreateDefaultProductRequest request = new CreateDefaultProductRequest(categoryId, "Kohinoor Basmati Rice", 1);
        when(catalogService.createProduct(eq(request)))
                .thenReturn(new AdminDefaultProductResponse(UUID.randomUUID(), categoryId, "Kohinoor Basmati Rice", 1, true, null));

        mockMvc.perform(post("/api/admin/default-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kohinoor Basmati Rice"));
    }

    @Test
    void update_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateDefaultProductRequest request = new UpdateDefaultProductRequest("India Gate Basmati Rice", 2, false);
        when(catalogService.updateProduct(eq(id), any()))
                .thenReturn(new AdminDefaultProductResponse(id, categoryId, "India Gate Basmati Rice", 2, false, null));

        mockMvc.perform(put("/api/admin/default-products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void uploadPhoto_delegatesToServiceAndReturnsUpdatedProduct() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "product.jpg", "image/jpeg", "x".getBytes());
        when(catalogService.uploadProductPhoto(eq(id), any()))
                .thenReturn(new AdminDefaultProductResponse(
                        id, categoryId, "Kohinoor Basmati Rice", 1, true, "/media/images/default-catalog/x/y.jpg"));

        mockMvc.perform(multipart("/api/admin/default-products/" + id + "/photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("/media/images/default-catalog/x/y.jpg"));
    }
}
