package com.company.casehub.capability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.capability.controller.CapabilityController;
import com.company.casehub.capability.dto.CapabilityResponse;
import com.company.casehub.capability.dto.CapabilityTreeNode;
import com.company.casehub.capability.service.CapabilityService;
import com.company.casehub.common.MethodSecurityTestConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RBAC contract of {@code /api/v1/capabilities} (Security & RBAC Detail V1.0 §33).
 *
 * <p>Reading is open to any authenticated user; every mutation requires
 * {@code capability:manage_library}, which ADMIN holds and TESTER does not (TESTER is
 * seeded with {@code capability:read} only) — that asymmetry is what these tests pin.
 *
 * <p>{@code @Import(MethodSecurityTestConfig.class)} is mandatory: {@code @WebMvcTest}
 * loads only the web slice, so the production {@code SecurityConfig} that carries
 * {@code @EnableMethodSecurity} is absent and {@code @PreAuthorize} would silently not
 * be enforced (mutations would return 2xx for a principal without the authority).
 */
@WebMvcTest(controllers = CapabilityController.class)
@Import(MethodSecurityTestConfig.class)
class CapabilityControllerRbacTest {

    private static final UUID CAPABILITY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CapabilityService capabilityService;

    // -------------------------------------------------------------- read (any authenticated user)

    @Test
    @DisplayName("TESTER shape (capability:read only) may read the tree")
    @WithMockUser(authorities = "capability:read")
    void readAllowedForAnyAuthenticatedUser() throws Exception {
        when(capabilityService.getTree()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/capabilities/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Reading the tree does not require capability:read")
    @WithMockUser(authorities = "unrelated:permission")
    void readAllowedWithoutCapabilityPermission() throws Exception {
        when(capabilityService.getTree()).thenReturn(List.of(
                new CapabilityTreeNode(CAPABILITY_ID, null, "BLUETOOTH", "Bluetooth", null, 0, true, List.of())));

        mockMvc.perform(get("/api/v1/capabilities/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BLUETOOTH"));
    }

    // -------------------------------------------------------------- create

    @Test
    @DisplayName("TESTER shape (capability:read only) is denied create -> 403")
    @WithMockUser(authorities = "capability:read")
    void createDeniedWithoutManagePermission() throws Exception {
        mockMvc.perform(post("/api/v1/capabilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN shape (capability:manage_library) may create -> 201")
    @WithMockUser(authorities = "capability:manage_library")
    void createAllowedWithManagePermission() throws Exception {
        when(capabilityService.create(any())).thenReturn(response(true));

        mockMvc.perform(post("/api/v1/capabilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BLE"));

        verify(capabilityService).create(any());
    }

    @Test
    @DisplayName("POST without a CSRF token is rejected before the permission check")
    @WithMockUser(authorities = "capability:manage_library")
    void createWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/capabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Create with an invalid body -> 400 (bean validation)")
    @WithMockUser(authorities = "capability:manage_library")
    void createWithBlankCodeIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/capabilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"  \",\"name\":\"BLE\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- update

    @Test
    @DisplayName("TESTER shape is denied update -> 403")
    @WithMockUser(authorities = "capability:read")
    void updateDeniedWithoutManagePermission() throws Exception {
        mockMvc.perform(put("/api/v1/capabilities/{capabilityId}", CAPABILITY_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN shape may update -> 200")
    @WithMockUser(authorities = "capability:manage_library")
    void updateAllowedWithManagePermission() throws Exception {
        when(capabilityService.update(eq(CAPABILITY_ID), any())).thenReturn(response(true));

        mockMvc.perform(put("/api/v1/capabilities/{capabilityId}", CAPABILITY_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CAPABILITY_ID.toString()));

        verify(capabilityService).update(eq(CAPABILITY_ID), any());
    }

    // -------------------------------------------------------------- enable

    @Test
    @DisplayName("TESTER shape is denied enable -> 403")
    @WithMockUser(authorities = "capability:read")
    void enableDeniedWithoutManagePermission() throws Exception {
        mockMvc.perform(post("/api/v1/capabilities/{capabilityId}/enable", CAPABILITY_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN shape may enable -> 200")
    @WithMockUser(authorities = "capability:manage_library")
    void enableAllowedWithManagePermission() throws Exception {
        when(capabilityService.enable(CAPABILITY_ID)).thenReturn(response(true));

        mockMvc.perform(post("/api/v1/capabilities/{capabilityId}/enable", CAPABILITY_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(capabilityService).enable(CAPABILITY_ID);
    }

    // -------------------------------------------------------------- disable

    @Test
    @DisplayName("TESTER shape is denied disable -> 403")
    @WithMockUser(authorities = "capability:read")
    void disableDeniedWithoutManagePermission() throws Exception {
        mockMvc.perform(post("/api/v1/capabilities/{capabilityId}/disable", CAPABILITY_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN shape may disable -> 200")
    @WithMockUser(authorities = "capability:manage_library")
    void disableAllowedWithManagePermission() throws Exception {
        when(capabilityService.disable(CAPABILITY_ID)).thenReturn(response(false));

        mockMvc.perform(post("/api/v1/capabilities/{capabilityId}/disable", CAPABILITY_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(capabilityService).disable(CAPABILITY_ID);
    }

    // -------------------------------------------------------------- helpers

    private static String createBody() {
        return "{\"code\":\"BLE\",\"name\":\"BLE\",\"description\":\"Low Energy\",\"sortOrder\":0}";
    }

    private static CapabilityResponse response(boolean enabled) {
        return new CapabilityResponse(
                CAPABILITY_ID, null, "BLE", "BLE", "Low Energy", 0, enabled,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
