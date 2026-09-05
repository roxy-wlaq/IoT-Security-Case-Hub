package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.user.entity.RoleEntity;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.entity.UserRoleEntity;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.repository.AuditRecordRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class AuthFlowIT extends AbstractIntegrationTest {

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
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Shared server-side session so the authenticated principal and CSRF token survive across requests. */
    private MockHttpSession session;

    private String username;
    private static final String PASSWORD = "Password123!@#";

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = new UserEntity(username, "Test User", passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
        RoleEntity admin = roleRepository.findByCode("ADMIN").orElseThrow();
        userRoleRepository.save(new UserRoleEntity(user, admin));
    }

    /** Attach the shared session + a valid CSRF token to a mutating request. */
    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.session(session).with(csrf());
    }

    @Test
    void csrfEndpointReturnsTokenAndCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.cookieName").value("XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(authed(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "wrong-pass")))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        assertThat(auditRecordRepository.findAll().stream()
                .filter(row -> row.getAction() == AuditAction.LOGIN_FAILURE)
                .filter(row -> username.equals(row.getActorUsername()))).hasSize(1);
    }

    @Test
    void fullLoginFlowReturnsUserWithPermissions() throws Exception {
        mockMvc.perform(authed(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.permissions").isArray());

        assertThat(auditRecordRepository.findAll().stream()
                .filter(row -> row.getAction() == AuditAction.LOGIN)
                .filter(row -> username.equals(row.getActorUsername()))).hasSize(1);

        // ADMIN seed must surface through the role_permissions mapping.
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.permissions").isArray());

        mockMvc.perform(authed(post("/api/v1/auth/logout")))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginLockoutAfterFiveFailuresReturns429() throws Exception {
        // First five failed attempts return 401 (AUTH_INVALID_CREDENTIALS).
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(authed(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "bad")))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        }
        // The next (6th) attempt after five failures is blocked with 429.
        mockMvc.perform(authed(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "bad")))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_TEMPORARILY_BLOCKED"));
    }

    @Test
    void changePasswordFlow() throws Exception {
        mockMvc.perform(authed(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD)))))
                .andExpect(status().isOk());

        String newPassword = "NewPassword456!@#";
        mockMvc.perform(authed(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", PASSWORD, "newPassword", newPassword)))))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", newPassword)))))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordWithWrongCurrentReturns400() throws Exception {
        mockMvc.perform(authed(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD)))))
                .andExpect(status().isOk());

        mockMvc.perform(authed(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "not-the-right-one", "newPassword", "AnotherPass789!@#")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_CURRENT_PASSWORD_MISMATCH"));
    }
}
