package com.xerxes.msme.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xerxes.msme.admin.dto.AdminAdvertisementResponse;
import com.xerxes.msme.admin.dto.CreateAdvertisementRequest;
import com.xerxes.msme.admin.dto.UpdateAdvertisementRequest;
import com.xerxes.msme.marketplace.AdvertisementSlot;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

@WebMvcTest(controllers = AdminAdvertisementController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAdvertisementControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminAdvertisementService advertisementService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID id = UUID.randomUUID();
    // UTC on purpose: Jackson reads timestamps back as UTC, so a local-offset value would never equal the parsed request.
    private final OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC);
    private final OffsetDateTime endsAt = startsAt.plusDays(7);

    private AdminAdvertisementResponse response(boolean published) {
        return new AdminAdvertisementResponse(
                id, AdvertisementSlot.LEFT_RAIL, "/media/images/advertisements/" + id + "/banner.jpg",
                "https://example.com", "alt text", "Big sale", "50% off", published, startsAt, endsAt, published);
    }

    @Test
    void list_delegatesToServiceForTheGivenSlot() throws Exception {
        when(advertisementService.listBySlot(AdvertisementSlot.LEFT_RAIL)).thenReturn(List.of(response(false)));

        mockMvc.perform(get("/api/admin/advertisements").param("slot", "LEFT_RAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slot").value("LEFT_RAIL"));
    }

    @Test
    void create_delegatesToServiceAndReturns201() throws Exception {
        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                AdvertisementSlot.LEFT_RAIL, "https://example.com", "alt text", "Big sale", "50% off", startsAt, endsAt);
        when(advertisementService.create(request)).thenReturn(response(false));

        mockMvc.perform(post("/api/admin/advertisements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slot").value("LEFT_RAIL"));
    }

    @Test
    void update_delegatesToService() throws Exception {
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                "https://example.com", "alt text", "Big sale", "50% off", startsAt, endsAt);
        when(advertisementService.update(eq(id), any())).thenReturn(response(false));

        mockMvc.perform(put("/api/admin/advertisements/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void uploadPhoto_delegatesToService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", "bytes".getBytes());
        when(advertisementService.uploadPhoto(eq(id), any())).thenReturn(response(false));

        mockMvc.perform(multipart("/api/admin/advertisements/" + id + "/photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("/media/images/advertisements/" + id + "/banner.jpg"));
    }

    @Test
    void publish_delegatesToServiceWithPublishedTrue() throws Exception {
        when(advertisementService.setPublished(id, true)).thenReturn(response(true));

        mockMvc.perform(put("/api/admin/advertisements/" + id + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void unpublish_delegatesToServiceWithPublishedFalse() throws Exception {
        when(advertisementService.setPublished(id, false)).thenReturn(response(false));

        mockMvc.perform(put("/api/admin/advertisements/" + id + "/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void delete_delegatesToServiceAndReturns204() throws Exception {
        mockMvc.perform(delete("/api/admin/advertisements/" + id)).andExpect(status().isNoContent());
    }
}
