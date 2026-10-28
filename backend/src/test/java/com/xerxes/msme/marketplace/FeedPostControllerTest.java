package com.xerxes.msme.marketplace;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.marketplace.dto.CreateFeedPostRequest;
import com.xerxes.msme.marketplace.dto.FeedPostResponse;
import com.xerxes.msme.security.AuthContext;
import com.xerxes.msme.security.AuthenticatedPrincipal;
import com.xerxes.msme.user.Role;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeedPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeedPostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FeedPostService feedPostService;
    @MockBean private AuthContext authContext;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        when(authContext.current())
                .thenReturn(new AuthenticatedPrincipal(UUID.randomUUID(), tenantId, "a@b.com", Role.OWNER, UUID.randomUUID()));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        FeedPostResponse response = new FeedPostResponse(
                UUID.randomUUID(), "50% off today!", null, null, FeedPostStatus.PENDING, null, OffsetDateTime.now());
        when(feedPostService.create(eq(tenantId), org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/feed-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedPostRequest("50% off today!", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void create_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/feed-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedPostRequest(" ", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_contentOver140Characters_returns400() throws Exception {
        String tooLong = "x".repeat(141);

        mockMvc.perform(post("/api/feed-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedPostRequest(tooLong, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mine_returnsTheCurrentTenantsOwnPosts() throws Exception {
        when(feedPostService.listMine(tenantId))
                .thenReturn(List.of(new FeedPostResponse(
                        UUID.randomUUID(), "Hello", "20% off", java.time.LocalDate.of(2030, 1, 15), FeedPostStatus.APPROVED, null,
                        OffsetDateTime.now())));

        mockMvc.perform(get("/api/feed-posts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello"))
                .andExpect(jsonPath("$[0].discountLabel").value("20% off"))
                .andExpect(jsonPath("$[0].validUntil").value("2030-01-15"));
    }

    @Test
    void create_discountLabelOver30Characters_returns400() throws Exception {
        mockMvc.perform(post("/api/feed-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFeedPostRequest("Sale", "x".repeat(31), null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withDiscountLabelAndEndDate_passesThemToTheService() throws Exception {
        java.time.LocalDate endDate = java.time.LocalDate.of(2030, 3, 9);
        when(feedPostService.create(eq(tenantId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new FeedPostResponse(
                        UUID.randomUUID(), "Winter sale", "Buy 1 Get 1", endDate, FeedPostStatus.PENDING, null,
                        OffsetDateTime.now()));

        mockMvc.perform(post("/api/feed-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFeedPostRequest("Winter sale", "Buy 1 Get 1", endDate))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountLabel").value("Buy 1 Get 1"))
                .andExpect(jsonPath("$.validUntil").value("2030-03-09"));

        org.mockito.ArgumentCaptor<CreateFeedPostRequest> captor =
                org.mockito.ArgumentCaptor.forClass(CreateFeedPostRequest.class);
        org.mockito.Mockito.verify(feedPostService).create(eq(tenantId), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().discountLabel()).isEqualTo("Buy 1 Get 1");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().validUntil()).isEqualTo(endDate);
    }
}
