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
        Attempt a = attempts.get(key);
        if (a == null) {
            return false;
        }
        if (a.blockedUntil() != null && Instant.now().isBefore(a.blockedUntil())) {
            return true;
        }
        // Window expired: reset.
        if (a.blockedUntil() != null) {
            attempts.remove(key);
        }
        return false;
    }

    public void recordFailure(String key) {
        Attempt current = attempts.getOrDefault(key, new Attempt(0, null));
        int next = current.count() + 1;
        if (next >= MAX_ATTEMPTS) {
            attempts.put(key, new Attempt(next, Instant.now().plus(BLOCK_DURATION)));
        } else {
            attempts.put(key, new Attempt(next, null));
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }
}
