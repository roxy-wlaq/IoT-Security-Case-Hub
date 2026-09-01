package com.company.casehub.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.auth.service.SessionRegistryService;
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
 * HIGH-01 regression: an expired session (admin disable / password reset via
 * {@link SessionRegistryService#expireSessions}) must be rejected on the next
 * request instead of silently staying authenticated.
 */
@AutoConfigureMockMvc
class SessionExpirationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRegistryService sessionRegistryService;

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

    private UUID userId;

    private static final String PASSWORD = "Password123!@#";

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        username = "expire_" + UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = new UserEntity(username, "Expire User", passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
        userId = user.getId();
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
    void expiredSessionIsRejectedOnNextRequest() throws Exception {
        login();

        // Administrator disables the user / resets password -> session marked expired.
        sessionRegistryService.expireSessions(userId);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validSessionStillWorksBeforeExpiry() throws Exception {
        login();

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }
}
