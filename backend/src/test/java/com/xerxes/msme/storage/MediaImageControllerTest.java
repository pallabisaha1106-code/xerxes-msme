package com.xerxes.msme.storage;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MediaImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaImageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ImageStorageService imageStorageService;

    @Test
    void get_existingObject_streamsBytesWithItsContentType() throws Exception {
        byte[] bytes = "fake-jpeg-bytes".getBytes();
        when(imageStorageService.downloadWithContentType("tenant-1/marketplace/cover.jpg"))
                .thenReturn(new ImageStorageService.StoredImage(bytes, "image/jpeg"));

        mockMvc.perform(get("/media/images/tenant-1/marketplace/cover.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void get_missingObject_returns404() throws Exception {
        when(imageStorageService.downloadWithContentType("does/not/exist.jpg")).thenReturn(null);

        mockMvc.perform(get("/media/images/does/not/exist.jpg")).andExpect(status().isNotFound());
    }

    @Test
    void get_objectWithNoContentType_fallsBackToOctetStream() throws Exception {
        byte[] bytes = "bytes".getBytes();
        when(imageStorageService.downloadWithContentType("tenant-1/kyc/owner-id.jpg"))
                .thenReturn(new ImageStorageService.StoredImage(bytes, null));

        mockMvc.perform(get("/media/images/tenant-1/kyc/owner-id.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
    }

    @Test
    void get_aKeptOriginal_isNeverServed() throws Exception {
        mockMvc.perform(get("/media/images/tenant-1/product-originals/variant-1/abc")).andExpect(status().isNotFound());

        verify(imageStorageService, never()).downloadWithContentType(org.mockito.ArgumentMatchers.any());
    }
}
