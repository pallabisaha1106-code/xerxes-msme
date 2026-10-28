package com.xerxes.msme.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicApiRateLimiterTest {

    @Test
    void tryAcquire_underLimit_allowsRequest() {
        PublicApiRateLimiter limiter = new PublicApiRateLimiter();

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
    }

    @Test
    void tryAcquire_atLimit_rejectsFurtherRequestsForSameKey() {
        PublicApiRateLimiter limiter = new PublicApiRateLimiter();

        for (int i = 0; i < 30; i++) {
            assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        }

        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse();
    }

    @Test
    void tryAcquire_differentKeys_trackedIndependently() {
        PublicApiRateLimiter limiter = new PublicApiRateLimiter();

        for (int i = 0; i < 30; i++) {
            limiter.tryAcquire("1.2.3.4");
        }

        assertThat(limiter.tryAcquire("5.6.7.8")).isTrue();
    }
}
