package com.xerxes.msme.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xerxes.msme.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-to-server only - authenticated by Razorpay's HMAC signature, not
 * Spring Security (see SecurityConfig's permitAll for this path, deliberately
 * outside /api/public/**'s wide-open CORS bucket, which is irrelevant here).
 */
@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);
    private static final String PAYMENT_LINK_PAID_EVENT = "payment_link.paid";

    private final RazorpayClient razorpayClient;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookController(
            RazorpayClient razorpayClient, SubscriptionService subscriptionService, ObjectMapper objectMapper) {
        this.razorpayClient = razorpayClient;
        this.subscriptionService = subscriptionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Signature") String signature,
            HttpServletRequest request) {
        if (!razorpayClient.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected Razorpay webhook with an invalid signature");
            // Same ApiError shape as the rest of the API, even though the only reader of this
            // response is Razorpay's own retry logic, not a browser - consistency over a bare
            // empty-body 401, and gives something useful if this is ever inspected by hand.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of(
                            HttpStatus.UNAUTHORIZED.value(),
                            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                            "Invalid webhook signature",
                            request.getRequestURI()));
        }

        JsonNode json = parse(rawBody);
        String event = json.path("event").asText();
        if (PAYMENT_LINK_PAID_EVENT.equals(event)) {
            String paymentLinkId =
                    json.path("payload").path("payment_link").path("entity").path("id").asText();
            subscriptionService.handleWebhookPaid(paymentLinkId);
        }

        return ResponseEntity.ok().build();
    }

    private JsonNode parse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed Razorpay webhook payload", ex);
        }
    }
}
