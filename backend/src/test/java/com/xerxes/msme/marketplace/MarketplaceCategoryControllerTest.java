package com.xerxes.msme.marketplace;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MarketplaceCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class MarketplaceCategoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private MarketplaceCategoryRepository categoryRepository;

    @Test
    void list_returnsActiveCategoriesInDisplayOrder() throws Exception {
        MarketplaceCategory category = new MarketplaceCategory();
        category.setId(UUID.randomUUID());
        category.setName("Ladies Wear");
        category.setIcon("♧");
        when(categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/marketplace/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ladies Wear"));
    }
}
