package com.company.casehub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LoginAttemptService.class)
@TestPropertySource(properties = {
        "casehub.security.login.max-failures=2",
        "casehub.security.login.block-duration=1m"
})
class LoginAttemptServiceSpringConfigTest {

    @Autowired
    private LoginAttemptService service;

    @Test
    void springInjectsConfiguredFailureThreshold() {
        String key = "configured-user|198.51.100.10";

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isFalse();

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isTrue();
    }
}
