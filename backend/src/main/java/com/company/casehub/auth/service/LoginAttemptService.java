package com.company.casehub.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory login failure tracker (V1). Keyed by {@code username + '|' + ip}.
 * After {@code maxAttempts} failures the key is locked for {@code blockDuration}.
 * Does NOT reveal the remaining attempt count (per security rule).
 *
 * <p>Note: state is process-local; this is acceptable for a single-instance V1
 * deployment. A shared store would be required for horizontal scaling.</p>
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int count, Instant blockedUntil) {
    }

    public String key(String username, String ip) {
        return (username == null ? "" : username.toLowerCase()) + "|" + (ip == null ? "" : ip);
    }

    public boolean isBlocked(String key) {
        // Atomically read-and-reset: a live block is kept, an expired window is removed.
        Attempt a = attempts.compute(key, (k, v) -> {
            if (v == null) {
                return null;
            }
            if (v.blockedUntil() != null && Instant.now().isBefore(v.blockedUntil())) {
                return v;
            }
            return null;
        });
        return a != null;
    }

    public void recordFailure(String key) {
        // Atomic read-modify-write so concurrent failures cannot lose a count.
        attempts.compute(key, (k, v) -> {
            int next = (v == null ? 0 : v.count()) + 1;
            if (next >= MAX_ATTEMPTS) {
                return new Attempt(next, Instant.now().plus(BLOCK_DURATION));
            }
            return new Attempt(next, null);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }
}
