package com.xerxes.msme.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over the two Razorpay REST operations this app needs - no
 * official SDK dependency pulled in for just these. See
 * https://razorpay.com/docs/api/payments/payment-links/ and
 * https://razorpay.com/docs/webhooks/validate-test/.
 */
@Component
public class RazorpayClient {

    private static final String PAYMENT_LINKS_URL = "https://api.razorpay.com/v1/payment_links";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RazorpayProperties properties;

    public RazorpayClient(ObjectMapper objectMapper, RazorpayProperties properties) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public record PaymentLink(String id, String shortUrl) {}

    public PaymentLink createPaymentLink(
            long amountPaise, String description, String referenceId, String customerEmail, String customerPhone) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("email", customerEmail);
        customer.put("contact", customerPhone);

        Map<String, Object> notify = Map.of("sms", true, "email", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", "INR");
        body.put("description", description);
        body.put("reference_id", referenceId);
        body.put("customer", customer);
        body.put("notify", notify);
        body.put("reminder_enable", true);

        String response = restClient
                .post()
                .uri(PAYMENT_LINKS_URL)
                .headers(headers -> headers.setBasicAuth(properties.keyId(), properties.keySecret()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode json = readTree(response);
        return new PaymentLink(json.path("id").asText(), json.path("short_url").asText());
    }

    /**
     * Razorpay signs each webhook delivery with HMAC-SHA256 over the raw
     * request body, using the webhook secret configured in the Razorpay
     * dashboard, sent back as the X-Razorpay-Signature header (hex-encoded).
     * MessageDigest.isEqual is constant-time - a plain String.equals would
     * leak timing information about how many leading bytes matched.
     */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (!StringUtils.hasText(signatureHeader)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.webhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().parseHex(signatureHeader.trim());
            return MessageDigest.isEqual(computed, expected);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException ex) {
            return false;
        }
    }

    private JsonNode readTree(String response) {
        try {
            return objectMapper.readTree(response);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Razorpay response: " + response, ex);
        }
    }
}
