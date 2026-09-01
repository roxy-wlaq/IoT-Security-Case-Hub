package com.company.casehub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void notBlockedBeforeThreshold() {
        String key = "user|10.0.0.1";
        for (int i = 0; i < 4; i++) {
            service.recordFailure(key);
        }
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void blockedAfterMaxFailures() {
        String key = "user|10.0.0.2";
        for (int i = 0; i < 5; i++) {
            service.recordFailure(key);
        }
        assertThat(service.isBlocked(key)).isTrue();
    }

    @Test
    void successResetsCounter() {
        String key = "user|10.0.0.3";
        service.recordFailure(key);
        service.recordFailure(key);
        service.recordSuccess(key);
        assertThat(service.isBlocked(key)).isFalse();
        // a single subsequent failure must not block
        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void keyNormalizesUsernameCase() {
        assertThat(service.key("Alice", "ip")).isEqualTo(service.key("alice", "ip"));
    }

    /**
     * Mirrors the real login flow: {@code isBlocked} is consulted before every
     * {@code recordFailure}. isBlocked must NOT reset the counter, otherwise the
     * 5-failure lock can never be reached.
     */
    @Test
    void isBlockedDoesNotResetCounterAcrossAttempts() {
        String key = "user|10.0.0.9";
        // isBlocked -> recordFailure, repeated 5 times (the threshold).
        for (int i = 0; i < 5; i++) {
            assertThat(service.isBlocked(key)).isFalse();
            service.recordFailure(key);
        }
        // The next consultation after five failures must report blocked.
        assertThat(service.isBlocked(key)).isTrue();
    }
}
