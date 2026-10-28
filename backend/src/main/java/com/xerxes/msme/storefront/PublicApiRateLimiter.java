package com.xerxes.msme.storefront;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple in-memory fixed-window rate limiter (per client IP) for the public,
 * unauthenticated storefront API - the one part of the platform reachable
 * without a login, so it needs its own abuse guard. Single-instance only
 * (no shared state across app instances); fine for this app's current scale.
 *
 * <p>Deliberately a plain class, not a {@code @Component}: it's owned by
 * WebConfig (see there for why), which gets pulled into every
 * {@code @WebMvcTest} slice - a real Spring bean here would need to resolve
 * in those slices too, the same problem StorageProperties had.
 */
public class PublicApiRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentHashMap<String, Deque<Instant>> hitsByKey = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Deque<Instant> hits = hitsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(now.minusMillis(WINDOW_MILLIS))) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }
}
