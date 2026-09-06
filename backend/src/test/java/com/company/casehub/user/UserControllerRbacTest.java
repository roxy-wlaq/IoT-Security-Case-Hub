package com.company.casehub.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.MethodSecurityTestConfig;
import com.company.casehub.testcase.dto.PagedResponse;
import com.company.casehub.user.controller.UserController;
import com.company.casehub.user.dto.UserCreatedResponse;
import com.company.casehub.user.dto.UserPasswordResetResponse;
import com.company.casehub.user.dto.UserSummaryResponse;
import com.company.casehub.user.service.UserManagementService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * RBAC matrix for {@link UserController}: every endpoint needs the ADMIN role
 * AND its frozen V002 permission. Mirrors {@code AuditControllerRbacTest}.
 */
@WebMvcTest(controllers = UserController.class)
@Import(MethodSecurityTestConfig.class)
class UserControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserManagementService service;

    private static final String USER_ID = "3f2b7c1e-0000-4000-8000-000000000001";

    private static RequestPostProcessor principal(String role, String... permissions) {
        java.util.Set<String> roleSet = new java.util.HashSet<>(Set.of(role));
        java.util.Set<String> permissionSet = new java.util.HashSet<>(List.of(permissions));
        UserPrincipal p = new UserPrincipal(UUID.randomUUID(), "actor", "hash", "Actor",
                true, false, roleSet, permissionSet);
        return user(p);
    }

    @Test
    void adminWithUserReadMayListUsers() throws Exception {
        when(service.list(any(), any(), any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/users").with(principal("ADMIN", "user:read")))
                .andExpect(status().isOk());
    }

    @Test
    void userWithoutUserReadIsDeniedFromList() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(principal("ADMIN", "project:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminWithUserReadIsDeniedFromList() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(principal("TESTER", "user:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithUserCreateMayCreateUser() throws Exception {
        when(service.create(any(), any())).thenReturn(new UserCreatedResponse(
                UUID.fromString(USER_ID), "alice", "Alice", true, true, List.of("TESTER"), "pw"));

        mockMvc.perform(post("/api/v1/users").with(principal("ADMIN", "user:create")).with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"displayName\":\"Alice\",\"roles\":[\"TESTER\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void createUserWithoutUserCreatePermissionIsDenied() throws Exception {
        mockMvc.perform(post("/api/v1/users").with(principal("ADMIN", "user:read")).with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"displayName\":\"Alice\",\"roles\":[\"TESTER\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithUserUpdateMayUpdateDisplayName() throws Exception {
        when(service.update(eq(UUID.fromString(USER_ID)), any(), any())).thenReturn(new UserSummaryResponse(
                UUID.fromString(USER_ID), "alice", "New Name", true, false, List.of("TESTER")));

        mockMvc.perform(put("/api/v1/users/" + USER_ID).with(principal("ADMIN", "user:update")).with(csrf())
                        .contentType("application/json")
                        .content("{\"displayName\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    @Test
    void updateRolesRequiresRoleManage() throws Exception {
        when(service.updateRoles(eq(UUID.fromString(USER_ID)), any(), any())).thenReturn(new UserSummaryResponse(
                UUID.fromString(USER_ID), "alice", "Alice", true, false, List.of("TEST_COORDINATOR")));

        mockMvc.perform(put("/api/v1/users/" + USER_ID + "/roles").with(principal("ADMIN", "user:update")).with(csrf())
                        .contentType("application/json")
                        .content("{\"roles\":[\"TEST_COORDINATOR\"]}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/users/" + USER_ID + "/roles").with(principal("ADMIN", "role:manage")).with(csrf())
                        .contentType("application/json")
                        .content("{\"roles\":[\"TEST_COORDINATOR\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void enableDisableRequiresUserDisable() throws Exception {
        when(service.setEnabled(eq(UUID.fromString(USER_ID)), ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(new UserSummaryResponse(
                        UUID.fromString(USER_ID), "alice", "Alice", false, false, List.of("TESTER")));

        mockMvc.perform(post("/api/v1/users/" + USER_ID + "/disable").with(principal("ADMIN", "user:update")).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users/" + USER_ID + "/disable").with(principal("ADMIN", "user:disable")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void passwordResetRequiresUserUpdate() throws Exception {
        when(service.resetPassword(eq(UUID.fromString(USER_ID)), any(), any()))
                .thenReturn(new UserPasswordResetResponse(true, "temp-password-123", true));

        mockMvc.perform(post("/api/v1/users/" + USER_ID + "/password-reset")
                        .with(principal("ADMIN", "user:disable")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users/" + USER_ID + "/password-reset")
                        .with(principal("ADMIN", "user:update")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated").value(true));
    }
}
