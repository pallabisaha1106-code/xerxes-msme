package com.xerxes.msme.subscription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicMembershipPlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicMembershipPlanControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void plans_listsEveryPlanInOrderWithItsPriceAndProductLimit() throws Exception {
        mockMvc.perform(get("/api/public/membership-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].plan").value("FREE"))
                .andExpect(jsonPath("$[0].monthlyAmountPaise").value(0))
                .andExpect(jsonPath("$[0].productLimit").value(10))
                .andExpect(jsonPath("$[1].plan").value("PLUS"))
                .andExpect(jsonPath("$[1].monthlyAmountPaise").value(19900))
                .andExpect(jsonPath("$[1].productLimit").value(100))
                .andExpect(jsonPath("$[2].plan").value("PREMIUM"))
                .andExpect(jsonPath("$[2].monthlyAmountPaise").value(99900))
                .andExpect(jsonPath("$[2].productLimit").value(500));
    }
}
