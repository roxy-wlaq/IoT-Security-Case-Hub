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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MEDIUM (round 2): Session fixation protection. A login performed against an existing
 * anonymous session must produce a NEW session id (ChangeSessionIdAuthenticationStrategy)
 * and the previous id must no longer carry an authenticated context.
 *
 * <p>Runs against PostgreSQL Testcontainers (see {@link AbstractIntegrationTest}).</p>
 */
@AutoConfigureMockMvc
class SessionFixationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession anonSession;
    private String username;
    private static final String PASSWORD = "Password123!@#";

    @BeforeEach
    void setUp() {
        anonSession = new MockHttpSession();
        username = "fixation_" + UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = new UserEntity(username, "Fixation User", passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
        RoleEntity admin = roleRepository.findByCode("ADMIN").orElseThrow();
        userRoleRepository.save(new UserRoleEntity(user, admin));
    }

    @Test
    void loginChangesSessionIdAndInvalidatesPrevious() throws Exception {
        // 1. Establish an anonymous session (fetching the CSRF token creates/saves one).
        mockMvc.perform(get("/api/v1/auth/csrf").session(anonSession)).andExpect(status().isOk());
        String oldSessionId = anonSession.getId();

        // 2. Login using that same session.
        mockMvc.perform(post("/api/v1/auth/login")
                        .session(anonSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk());

        // 3. The session id must differ after login (fixation protection).
        String newSessionId = anonSession.getId();
        assertThat(newSessionId).isNotEqualTo(oldSessionId);

        // 4. The new session can access a protected resource.
        mockMvc.perform(get("/api/v1/auth/me").session(anonSession))
                .andExpect(status().isOk());

        // 5. The old session id no longer carries an authenticated context.
        MockHttpSession oldSession = new MockHttpSession(null, oldSessionId);
        mockMvc.perform(get("/api/v1/auth/me").session(oldSession))
                .andExpect(status().isUnauthorized());
    }
}
