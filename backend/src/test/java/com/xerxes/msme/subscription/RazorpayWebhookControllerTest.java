package com.xerxes.msme.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RazorpayWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class RazorpayWebhookControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private RazorpayClient razorpayClient;
    @MockBean private SubscriptionService subscriptionService;

    @Test
    void handleWebhook_invalidSignature_returns401WithApiErrorBody() throws Exception {
        when(razorpayClient.verifyWebhookSignature(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void handleWebhook_validSignatureButUnrecognizedEvent_returns200AndIgnoresIt() throws Exception {
        when(razorpayClient.verifyWebhookSignature(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "good-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"some.other.event\"}"))
                .andExpect(status().isOk());
    }
}
