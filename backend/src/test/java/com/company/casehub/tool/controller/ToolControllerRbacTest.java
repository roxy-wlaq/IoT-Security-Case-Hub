package com.company.casehub.tool.controller;

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
import com.company.casehub.tool.dto.ToolCreateRequest;
import com.company.casehub.tool.dto.ToolResponse;
import com.company.casehub.tool.dto.ToolUpdateRequest;
import com.company.casehub.tool.service.ToolService;
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
 * RBAC slice tests for the tool metadata endpoints.
 *
 * <p>Reads are open to any authenticated user; writes require the
 * {@code tool:manage} authority. {@link MethodSecurityTestConfig} restores
 * {@code @PreAuthorize} enforcement inside the {@code @WebMvcTest} slice.
 */
@WebMvcTest(controllers = ToolController.class)
@Import(MethodSecurityTestConfig.class)
class ToolControllerRbacTest {

    private static final String BASE_URL = "/api/v1/tools";
    private static final String MANAGE_AUTHORITY = "tool:manage";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolService service;

    private static ToolResponse sampleResponse() {
        Instant now = Instant.now();
        return new ToolResponse(UUID.randomUUID(), "TOOL-NMAP", "Nmap", "Port scanner", "Cross-platform", "https://nmap.org", true, now, now);
    }

    @Test
    void readIsOpenToAnyAuthenticatedUser() throws Exception {
        when(service.list(null, null)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(BASE_URL).with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TOOL-NMAP"));

        verify(service).list(null, null);
    }

    @Test
    void readPassesSearchFiltersToService() throws Exception {
        when(service.list("nmap", true)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).with(user("reader"))
                        .param("search", "nmap")
                        .param("enabled", "true"))
                .andExpect(status().isOk());

        verify(service).list("nmap", true);
    }

    @Test
    void readByIdIsOpenToAnyAuthenticatedUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(sampleResponse());

        mockMvc.perform(get(BASE_URL + "/{id}", id).with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nmap"));

        verify(service).get(id);
    }

    @Test
    void createIsForbiddenWithoutManageAuthority() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user("reader")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"code":"TOOL-NMAP","name":"Nmap"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any(ToolCreateRequest.class));
    }

    @Test
    void createWithManageAuthorityReturns201() throws Exception {
        when(service.create(any(ToolCreateRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post(BASE_URL).with(user("manager").authorities(() -> MANAGE_AUTHORITY)).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"code":"TOOL-NMAP","name":"Nmap"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TOOL-NMAP"));

        verify(service).create(any(ToolCreateRequest.class));
    }

    @Test
    void updateIsForbiddenWithoutManageAuthority() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", UUID.randomUUID()).with(user("reader")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"Renamed"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).update(any(UUID.class), any(ToolUpdateRequest.class));
    }

    @Test
    void updateWithManageAuthorityReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(any(UUID.class), any(ToolUpdateRequest.class))).thenReturn(sampleResponse());

        MockHttpServletRequestBuilder request = put(BASE_URL + "/{id}", id)
                .with(user("manager").authorities(() -> MANAGE_AUTHORITY)).with(csrf())
                .contentType("application/json")
                .content("""
                        {"enabled":false}
                        """);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOOL-NMAP"));

        verify(service).update(eq(id), any(ToolUpdateRequest.class));
    }
}
