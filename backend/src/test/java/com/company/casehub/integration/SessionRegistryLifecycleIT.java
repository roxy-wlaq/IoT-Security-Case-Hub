package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.user.repository.RoleRepository;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MEDIUM (round 2): SessionRegistry lifecycle. With {@code HttpSessionEventPublisher}
 * registered, {@link SessionRegistry} must drop stale {@code SessionInformation} after
 * logout / invalidation / expire, so disabled/expired users cannot keep a ghost session.
 *
 * <p>Runs against PostgreSQL Testcontainers (see {@link AbstractIntegrationTest}).</p>
 */
@AutoConfigureMockMvc
class SessionRegistryLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession session;
    private String username;
    private static final String PASSWORD = "Password123!@#";

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        username = "lifecycle_" + UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = new UserEntity(username, "Lifecycle User", passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
        RoleEntity admin = roleRepository.findByCode("ADMIN").orElseThrow();
        userRoleRepository.save(new UserRoleEntity(user, admin));
    }

    private void login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void sessionInfoRemovedAfterLogout() throws Exception {
        login();
        String sessionId = session.getId();
        assertThat(sessionRegistry.getSessionInformation(sessionId)).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        // HttpSessionEventPublisher -> SessionRegistryImpl drops the invalidated session.
        assertThat(sessionRegistry.getSessionInformation(sessionId)).isNull();
    }

    @Test
    void sessionInfoRemovedAfterServerSideExpire() throws Exception {
        login();
        String sessionId = session.getId();
        assertThat(sessionRegistry.getSessionInformation(sessionId)).isNotNull();

        // Admin disable / password reset marks the session expired; the next request
        // (enforced by SessionExpiryFilter) removes the entry from the registry.
        sessionRegistry.getSessionInformation(sessionId).expireNow();
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());

        assertThat(sessionRegistry.getSessionInformation(sessionId)).isNull();
    }
}
