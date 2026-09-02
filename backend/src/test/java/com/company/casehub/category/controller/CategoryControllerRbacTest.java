package com.company.casehub.category.controller;

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

import com.company.casehub.category.dto.CategoryCreateRequest;
import com.company.casehub.category.dto.CategoryResponse;
import com.company.casehub.category.dto.CategoryUpdateRequest;
import com.company.casehub.category.service.CategoryService;
import com.company.casehub.common.MethodSecurityTestConfig;
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
 * RBAC slice tests for the category dictionary endpoints.
 *
 * <p>Reads are open to any authenticated user; writes require the
 * {@code category:manage} authority. {@link MethodSecurityTestConfig} restores
 * {@code @PreAuthorize} enforcement inside the {@code @WebMvcTest} slice.
 */
@WebMvcTest(controllers = CategoryController.class)
@Import(MethodSecurityTestConfig.class)
class CategoryControllerRbacTest {

    private static final String BASE_URL = "/api/v1/categories";
    private static final String MANAGE_AUTHORITY = "category:manage";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService service;

    private static CategoryResponse sampleRoot() {
        Instant now = Instant.now();
        return new CategoryResponse(UUID.randomUUID(), null, "CAT-ROOT", "Network Protocols", 1, null, 0, true, now, now, List.of());
    }

    @Test
    void treeIsOpenToAnyAuthenticatedUser() throws Exception {
        when(service.tree(null, null)).thenReturn(List.of(sampleRoot()));

        mockMvc.perform(get(BASE_URL + "/tree").with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CAT-ROOT"));

        verify(service).tree(null, null);
    }

    @Test
    void treePassesSearchFiltersToService() throws Exception {
        when(service.tree("modbus", false)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/tree").with(user("reader"))
                        .param("search", "modbus")
                        .param("enabled", "false"))
                .andExpect(status().isOk());

        verify(service).tree("modbus", false);
    }

    @Test
    void createIsForbiddenWithoutManageAuthority() throws Exception {
        mockMvc.perform(post(BASE_URL).with(user("reader")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"code":"CAT-ROOT","name":"Network Protocols"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any(CategoryCreateRequest.class));
    }

    @Test
    void createWithManageAuthorityReturns201() throws Exception {
        when(service.create(any(CategoryCreateRequest.class))).thenReturn(sampleRoot());

        mockMvc.perform(post(BASE_URL).with(user("manager").authorities(() -> MANAGE_AUTHORITY)).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"code":"CAT-ROOT","name":"Network Protocols"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value(1));

        verify(service).create(any(CategoryCreateRequest.class));
    }

    @Test
    void updateIsForbiddenWithoutManageAuthority() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}", UUID.randomUUID()).with(user("reader")).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"Renamed"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).update(any(UUID.class), any(CategoryUpdateRequest.class));
    }

    @Test
    void updateWithManageAuthorityReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(any(UUID.class), any(CategoryUpdateRequest.class))).thenReturn(sampleRoot());

        MockHttpServletRequestBuilder request = put(BASE_URL + "/{id}", id)
                .with(user("manager").authorities(() -> MANAGE_AUTHORITY)).with(csrf())
                .contentType("application/json")
                .content("""
                        {"enabled":false}
                        """);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CAT-ROOT"));

        verify(service).update(eq(id), any(CategoryUpdateRequest.class));
    }
}
