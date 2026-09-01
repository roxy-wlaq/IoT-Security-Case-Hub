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
        // Do NOT delete an entry that has not reached the lock threshold: the real
        // login flow calls isBlocked() before every attempt, so removing the entry
        // here would reset the failure counter on each call and the lock could never
        // be reached. Only a block window that has already expired is cleared.
        Attempt a = attempts.computeIfPresent(key, (k, v) -> {
            if (v.blockedUntil() == null) {
                return v; // not yet blocked -> keep the count alive
            }
            if (Instant.now().isBefore(v.blockedUntil())) {
                return v; // still inside the block window -> keep
            }
            return null; // block window expired -> remove
        });
        return a != null && a.blockedUntil() != null;
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
