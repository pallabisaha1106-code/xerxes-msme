package com.xerxes.msme.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class RazorpayClientTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String BODY = "{\"event\":\"payment_link.paid\",\"payload\":{}}";

    private final RazorpayClient client =
            new RazorpayClient(new ObjectMapper(), new RazorpayProperties("key_id", "key_secret", WEBHOOK_SECRET));

    private String validSignatureFor(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void verifyWebhookSignature_validSignature_returnsTrue() throws Exception {
        assertThat(client.verifyWebhookSignature(BODY, validSignatureFor(BODY))).isTrue();
    }

    @Test
    void verifyWebhookSignature_tamperedBody_returnsFalse() throws Exception {
        String signatureForOriginalBody = validSignatureFor(BODY);

        assertThat(client.verifyWebhookSignature(BODY + "tampered", signatureForOriginalBody)).isFalse();
    }

    @Test
    void verifyWebhookSignature_wrongSignature_returnsFalse() {
        assertThat(client.verifyWebhookSignature(BODY, "0000000000000000000000000000000000000000000000000000000000000000"))
                .isFalse();
    }

    @Test
    void verifyWebhookSignature_missingSignature_returnsFalse() {
        assertThat(client.verifyWebhookSignature(BODY, "")).isFalse();
        assertThat(client.verifyWebhookSignature(BODY, null)).isFalse();
    }

    @Test
    void verifyWebhookSignature_malformedHexSignature_returnsFalseInsteadOfThrowing() {
        assertThat(client.verifyWebhookSignature(BODY, "not-hex")).isFalse();
    }
}
