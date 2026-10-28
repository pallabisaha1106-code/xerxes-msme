package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminMarketplaceCategoryResponse;
import com.xerxes.msme.admin.dto.UpdateMarketplaceCategoryBookableRequest;
import com.xerxes.msme.admin.dto.UpdateMarketplaceCategoryImageProfileRequest;
import com.xerxes.msme.admin.dto.UpdateMarketplaceCategoryStatusRequest;
import com.xerxes.msme.marketplace.ImageProfile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminMarketplaceCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminMarketplaceCategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminMarketplaceCategoryService categoryService;

    @Test
    void list_returnsEveryCategory() throws Exception {
        when(categoryService.listAll())
                .thenReturn(List.of(new AdminMarketplaceCategoryResponse(UUID.randomUUID(), "Grocery", "♦", 1, true, ImageProfile.COMPACT, false)));

        mockMvc.perform(get("/api/admin/marketplace-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Grocery"))
                .andExpect(jsonPath("$[0].bookable").value(false));
    }

    @Test
    void updateStatus_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.updateStatus(eq(id), eq(false)))
                .thenReturn(new AdminMarketplaceCategoryResponse(id, "Grocery", "♦", 1, false, ImageProfile.COMPACT, false));

        mockMvc.perform(put("/api/admin/marketplace-categories/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMarketplaceCategoryStatusRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateImageProfile_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.updateImageProfile(eq(id), eq(ImageProfile.DETAIL)))
                .thenReturn(new AdminMarketplaceCategoryResponse(id, "Ladies Wear", "♧", 1, true, ImageProfile.DETAIL, false));

        mockMvc.perform(put("/api/admin/marketplace-categories/" + id + "/image-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMarketplaceCategoryImageProfileRequest(ImageProfile.DETAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageProfile").value("DETAIL"));
    }

    @Test
    void updateImageProfile_missingProfile_isRejected() throws Exception {
        mockMvc.perform(put("/api/admin/marketplace-categories/" + UUID.randomUUID() + "/image-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBookable_delegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoryService.updateBookable(eq(id), eq(true)))
                .thenReturn(new AdminMarketplaceCategoryResponse(id, "Salon & Spa", "✂", 15, true, ImageProfile.COMPACT, true));

        mockMvc.perform(put("/api/admin/marketplace-categories/" + id + "/bookable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMarketplaceCategoryBookableRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookable").value(true));
    }
}
