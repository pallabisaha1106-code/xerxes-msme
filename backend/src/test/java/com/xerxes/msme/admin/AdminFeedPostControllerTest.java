package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.admin.dto.AdminFeedPostResponse;
import com.xerxes.msme.admin.dto.ReviewFeedPostRequest;
import com.xerxes.msme.marketplace.FeedPostStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminFeedPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFeedPostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AdminFeedPostService adminFeedPostService;

    private final UUID postId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    private AdminFeedPostResponse response(FeedPostStatus status) {
        return new AdminFeedPostResponse(
                postId, tenantId, "Veera Collections", "50% off today!", "50% off", java.time.LocalDate.of(2030, 1, 15),
                status, null, OffsetDateTime.now(), null);
    }

    @Test
    void list_noStatusParam_returnsAllPosts() throws Exception {
        when(adminFeedPostService.list(null)).thenReturn(List.of(response(FeedPostStatus.PENDING)));

        mockMvc.perform(get("/api/admin/feed-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessName").value("Veera Collections"))
                .andExpect(jsonPath("$[0].discountLabel").value("50% off"))
                .andExpect(jsonPath("$[0].validUntil").value("2030-01-15"));
    }

    @Test
    void list_withStatusParam_filtersByStatus() throws Exception {
        when(adminFeedPostService.list(FeedPostStatus.PENDING)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/feed-posts").param("status", "PENDING")).andExpect(status().isOk());
    }

    @Test
    void approve_delegatesToService() throws Exception {
        when(adminFeedPostService.approve(postId)).thenReturn(response(FeedPostStatus.APPROVED));

        mockMvc.perform(post("/api/admin/feed-posts/" + postId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reject_withReasonBody_passesReasonThrough() throws Exception {
        ReviewFeedPostRequest request = new ReviewFeedPostRequest("Prohibited item.");
        when(adminFeedPostService.reject(postId, "Prohibited item.")).thenReturn(response(FeedPostStatus.REJECTED));

        mockMvc.perform(post("/api/admin/feed-posts/" + postId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void reject_withoutBody_passesNullReason() throws Exception {
        when(adminFeedPostService.reject(eq(postId), isNull())).thenReturn(response(FeedPostStatus.REJECTED));

        mockMvc.perform(post("/api/admin/feed-posts/" + postId + "/reject")).andExpect(status().isOk());
    }
}
