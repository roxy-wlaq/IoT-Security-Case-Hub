package com.company.casehub.standard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.common.MethodSecurityTestConfig;
import com.company.casehub.standard.dto.StandardTaskTypeCreateRequest;
import com.company.casehub.standard.dto.StandardTaskTypeResponse;
import com.company.casehub.standard.dto.StandardTaskTypeUpdateRequest;
import com.company.casehub.standard.service.StandardTaskTypeService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * RBAC slice tests for the standard/task-type dictionary endpoints.
 *
 * <p>Reads are open to any authenticated user; writes require the
 * {@code standard:manage} authority. {@link MethodSecurityTestConfig} restores
 * {@code @PreAuthorize} enforcement inside the {@code @WebMvcTest} slice.
 */
@WebMvcTest(controllers = StandardTaskTypeController.class)
@Import(MethodSecurityTestConfig.class)
class StandardTaskTypeControllerRbacTest {

    private static final String BASE_URL = "/api/v1/standard-task-types";
    private static final String MANAGE_AUTHORITY = "standard:manage";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StandardTaskTypeService service;

    private static StandardTaskTypeResponse sampleResponse() {
        Instant now = Instant.now();
        return new StandardTaskTypeResponse(UUID.randomUUID(), "STD-001", "USB Fuzzing", "STANDARD", null, true, now, now);
    }

    @Test
    void readIsOpenToAnyAuthenticatedUser() throws Exception {
        when(service.list(null, null, null)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(BASE_URL).with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("STD-001"));

        verify(service).list(null, null, null);
    }

    @Test
    void readPassesQueryFiltersToService() throws Exception {
        when(service.list("usb", true, "STANDARD")).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).with(user("reader"))
                        .param("q", "usb")
                        .param("enabled", "true")
                        .param("type", "STANDARD"))
                .andExpect(status().isOk());

        verify(service).list("usb", true, "STANDARD");
    }

    /**
     * The frozen contract uses {@code q}, not {@code search}. A caller sending the
     * legacy {@code search} parameter must NOT be silently honoured — the query
     * filter is then simply unset, which is the only way to catch a client that
     * still sends the old name.
     */
    @Test
    void legacySearchParameterIsNotBound() throws Exception {
        when(service.list(null, null, null)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(BASE_URL).with(user("reader")).param("search", "usb"))
                .andExpect(status().isOk());

        verify(service).list(null, null, null);
    }

    @Test
    void createIsForbiddenWithoutManageAuthority() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user("reader")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"code":"STD-001","name":"USB Fuzzing","type":"STANDARD"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any(StandardTaskTypeCreateRequest.class));
    }

    @Test
    void createWithManageAuthorityReturns201() throws Exception {
        when(service.create(any(StandardTaskTypeCreateRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post(BASE_URL).with(user("manager").authorities(() -> MANAGE_AUTHORITY)).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"code":"STD-001","name":"USB Fuzzing","type":"STANDARD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("STD-001"));

        verify(service).create(any(StandardTaskTypeCreateRequest.class));
    }

    @Test
    void updateIsForbiddenWithoutManageAuthority() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", UUID.randomUUID()).with(user("reader")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"Renamed"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).update(any(UUID.class), any(StandardTaskTypeUpdateRequest.class));
    }

    @Test
    void updateWithManageAuthorityReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(any(UUID.class), any(StandardTaskTypeUpdateRequest.class))).thenReturn(sampleResponse());

        MockHttpServletRequestBuilder request = put(BASE_URL + "/{id}", id)
                .with(user("manager").authorities(() -> MANAGE_AUTHORITY)).with(csrf())
                .contentType("application/json")
                .content("""
                        {"enabled":false}
                        """);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("STD-001"));

        verify(service).update(eq(id), any(StandardTaskTypeUpdateRequest.class));
    }
}
